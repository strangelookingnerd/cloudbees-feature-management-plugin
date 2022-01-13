package com.cloudbees.fm.jenkins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.rollout.configuration.Configuration;
import io.rollout.configuration.ConfigurationFetcher;
import io.rollout.configuration.comparison.ConfigurationComparator;
import io.rollout.configuration.comparison.ConfigurationComparisonResult;
import io.rollout.configuration.persistence.ConfigurationPersister;
import io.rollout.publicapi.PublicApi;
import io.rollout.publicapi.model.Application;
import io.rollout.publicapi.model.DataPersister;
import io.rollout.publicapi.model.Environment;
import io.rollout.publicapi.model.Flag;
import io.rollout.publicapi.model.TargetGroup;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * 
 */
public class FeatureManagementConfigurationBuilder extends Builder implements SimpleBuildStep {

    private String credentialsId;

    private Application application;
    private Environment environment;

    @DataBoundConstructor
    public FeatureManagementConfigurationBuilder(String credentialsId, String applicationId, String environmentId) {
        this.credentialsId = credentialsId;
        this.application = ((DescriptorImpl)getDescriptor()).applicationMap.get(applicationId);
        this.environment = ((DescriptorImpl)getDescriptor()).environmentMap.get(environmentId);
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getApplicationId() {
        return application.getId();
    }

    public String getEnvironmentId() {
        return environment.getKey();
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull Launcher launcher,
                        @NonNull TaskListener listener)
            throws IOException {

        try {
            run.addAction(new FeatureManagementConfigurationAction(application, environment));

            doPostPerformEmbeddedConfigurationActions(run, listener);
            doPostPerformPublicApiActions(run, listener);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void doPostPerformPublicApiActions(Run<?,?> run, TaskListener listener) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // Download and save the flags and target groups from the public API
        String apiToken = ((DescriptorImpl)getDescriptor()).getApiToken(credentialsId);
        List<Flag> flags = PublicApi.getInstance().getFlags(apiToken, application.getId(), environment.getName());
        List<TargetGroup> targetGroups = PublicApi.getInstance().getTargetGroups(apiToken, application.getId());

        // Save the flags and target groups to disk
        DataPersister.writeValue(run.getRootDir(), getEnvironmentId(), DataPersister.EntityType.FLAG, flags);
        DataPersister.writeValue(run.getRootDir(), getEnvironmentId(), DataPersister.EntityType.TARGET_GROUP, targetGroups);

        listener.getLogger().printf("From the Public API, there were %d flags and %d target groups", flags.size(), targetGroups.size());
    }

    private void doPostPerformEmbeddedConfigurationActions(Run<?, ?> run, TaskListener listener) throws IOException, ParseException {
        // Download and save the embedded configuration
        Configuration config = ConfigurationFetcher.getInstance().getConfiguration(environment.getKey());
        listener.getLogger().printf("Retrieved CloudBees Feature Management configuration for %s. %d Experiments, %d Target Groups. Last Updated: %s\n",
                environment, config.getExperiments().size(), config.getTargetGroups().size(), config.getSignedDate().toString());

        // Save the config
        ConfigurationPersister.getInstance().save(config, run, environment.getKey());

        // Awesome, we saved the config. Now load the config from the last successful build
        Run<?, ?> previousSuccessfulBuild = run.getPreviousSuccessfulBuild();
        if (previousSuccessfulBuild != null) {
            // read the file
            try {
                Configuration oldConfig = ConfigurationPersister.getInstance().load(previousSuccessfulBuild, environment.getKey());

                ConfigurationComparisonResult comparison = new ConfigurationComparator().compare(oldConfig, config);
                listener.getLogger().println("configs are " + (comparison.areEqual() ? "not " : "") + "different");

            } catch (Exception e) {
                listener.getLogger().printf("Could not load previous flag configuration from last successful build (%d)\n", previousSuccessfulBuild.getNumber());
            }
        } else {
            listener.getLogger().println("There were no previous successful build to compare the flag configurations to");
        }
    }

    @Symbol("featureManagementConfig")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class.getName());

        // A transient map of the ID->entity models, so that we can retrieve something that I've forgotten
        private final Map<String, Application> applicationMap = new HashMap<>();
        private final Map<String, Environment> environmentMap = new HashMap<>();
        private transient Set<String> validCredentialIds = new HashSet<>();
        private transient Set<String> invalidCredentialIds = new HashSet<>();

        @Override
        @NonNull
        public String getDisplayName() {
            return "CloudBees Feature Management Configuration";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public ListBoxModel doFillCredentialsIdItems(
                @AncestorInPath Item item,
                @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ)
                        && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }
            return result
                    .includeEmptyValue()
                    .includeMatchingAs(ACL.SYSTEM,
                            item,
                            StringCredentials.class,
                            // No domain requirements
                            URIRequirementBuilder.create().build(),
                            CredentialsMatchers.always())
                    .includeCurrentValue(credentialsId);
        }

        public FormValidation doCheckCredentialsId(@QueryParameter String credentialsId) {
            // Checking whether the credential is valid is a PITA as CBFM rate limits the API calls
            // Also, this method gets called many times and that overloads the API rate limits. Use a stored list of valid or invalid IDs.
            if (validCredentialIds.contains(credentialsId)) {
                return FormValidation.ok();
            } else if (invalidCredentialIds.contains(credentialsId)) {
                return FormValidation.error("API Token is invalid");
            } else {
                // We have not checked the credential yet. Check it now by listing applications
                try {
                    PublicApi.getInstance().listApplications(getApiToken(credentialsId));
                    validCredentialIds.add(credentialsId);
                    return FormValidation.ok();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "API token is invalid", e);
                    invalidCredentialIds.add(credentialsId);
                    return FormValidation.error("API token is invalid", e);
                }
            }
        }

        private String getApiToken(String credentialsId) {
            if (StringUtils.isBlank(credentialsId)) {
                throw new RuntimeException("No credentials Id");
            }

            List<StringCredentials> creds = CredentialsMatchers.filter(
                    CredentialsProvider.lookupCredentials(StringCredentials.class,
                            Jenkins.getInstance(), ACL.SYSTEM,
                            Collections.emptyList()),
                    CredentialsMatchers.withId(credentialsId)
            );

            return creds.stream().findFirst().map(c -> c.getSecret().getPlainText()).orElseThrow(() -> new RuntimeException("Could not find credential ID " + credentialsId));
        }

        public ListBoxModel doFillApplicationIdItems(@QueryParameter String credentialsId) throws IOException {
            if (StringUtils.isBlank(credentialsId) || invalidCredentialIds.contains(credentialsId)) {
                return null;
            }

            try {
                ListBoxModel items = new StandardListBoxModel().includeEmptyValue();

                PublicApi.getInstance().listApplications(getApiToken(credentialsId))
                        .forEach(application -> {
                            applicationMap.put(application.getId(), application);
                            items.add(application.getName(), application.getId());
                        });

                return items;
            } catch (Exception e) {
                return null;
            }
        }

        public ListBoxModel doFillEnvironmentIdItems(@QueryParameter String credentialsId, @QueryParameter String applicationId) throws IOException {
            if (StringUtils.isBlank(credentialsId) || invalidCredentialIds.contains(credentialsId) || StringUtils.isBlank(applicationId)) {
                return null;
            }

            try {
            ListBoxModel items = new StandardListBoxModel().includeEmptyValue();

            PublicApi.getInstance().listEnvironments(getApiToken(credentialsId), applicationId)
                    .forEach(environment -> {
                        environmentMap.put(environment.getKey(), environment);
                        items.add(environment.getName(), environment.getKey());
                    });

            return items;
            } catch (Exception e) {
                return null;
            }
        }

    }
}
