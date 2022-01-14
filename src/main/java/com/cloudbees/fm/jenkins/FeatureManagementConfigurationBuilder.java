package com.cloudbees.fm.jenkins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
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
import io.rollout.publicapi.PublicApi;
import io.rollout.publicapi.model.Application;
import io.rollout.publicapi.model.DataPersister;
import io.rollout.publicapi.model.Environment;
import io.rollout.publicapi.model.Flag;
import io.rollout.publicapi.model.TargetGroup;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

    private final String credentialsId;
    private final Application application;
    private final Environment environment;

    @DataBoundConstructor
    public FeatureManagementConfigurationBuilder(String credentialsId, String applicationIdAndName, String environmentIdAndName) {
        this.credentialsId = credentialsId;
        IdAndName appIdName = IdAndName.parse(applicationIdAndName);
        this.application = new Application(appIdName.getId(), appIdName.getName());
        IdAndName envIdName = IdAndName.parse(environmentIdAndName);
        this.environment = new Environment(envIdName.getId(), envIdName.getName(), null);
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getApplicationIdAndName() {
        return new IdAndName(application.getId(), application.getName()).toString();
    }
    public String getEnvironmentIdAndName() {
        return new IdAndName(environment.getKey(), environment.getName()).toString();
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull Launcher launcher,
                        @NonNull TaskListener listener)
            throws IOException {

        run.addAction(new FeatureManagementConfigurationAction(application, environment));

        doPostPerformPublicApiActions(run, listener);
    }

    private void doPostPerformPublicApiActions(Run<?,?> run, TaskListener listener) throws IOException {
        // Download and save the flags and target groups from the public API
        String apiToken = ((DescriptorImpl)getDescriptor()).getApiToken(credentialsId);
        List<Flag> flags = PublicApi.getInstance().getFlags(apiToken, application.getId(), environment.getName());
        List<TargetGroup> targetGroups = PublicApi.getInstance().getTargetGroups(apiToken, application.getId());

        // Save the flags and target groups to disk
        DataPersister.writeValue(run.getRootDir(), environment.getKey(), DataPersister.EntityType.FLAG, flags);
        DataPersister.writeValue(run.getRootDir(), environment.getKey(), DataPersister.EntityType.TARGET_GROUP, targetGroups);

        listener.getLogger().printf("From the Public API, there were %d flags and %d target groups", flags.size(), targetGroups.size());
    }

    @Symbol("featureManagementConfig")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class.getName());

        private final transient Set<String> validCredentialIds = new HashSet<>();
        private final transient Set<String> invalidCredentialIds = new HashSet<>();

        @Override
        @NonNull
        public String getDisplayName() {
            return "CloudBees Feature Management configuration";
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
            if (validCredentialIds.contains(credentialsId) || StringUtils.isBlank(credentialsId)) {
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

        public ListBoxModel doFillApplicationIdAndNameItems(@QueryParameter String credentialsId) throws IOException {
            if (StringUtils.isBlank(credentialsId) || invalidCredentialIds.contains(credentialsId)) {
                return null;
            }

            try {
                ListBoxModel items = new StandardListBoxModel().includeEmptyValue();

                PublicApi.getInstance().listApplications(getApiToken(credentialsId))
                        .forEach(application -> items.add(application.getName(), new IdAndName(application.getId(), application.getName()).toString()));

                return items;
            } catch (Exception e) {
                return null;
            }
        }

        public ListBoxModel doFillEnvironmentIdAndNameItems(@QueryParameter String credentialsId, @QueryParameter String applicationIdAndName) throws IOException {
            if (StringUtils.isBlank(credentialsId) || invalidCredentialIds.contains(credentialsId) || StringUtils.isBlank(applicationIdAndName)) {
                return null;
            }

            try {
            ListBoxModel items = new StandardListBoxModel().includeEmptyValue();

            PublicApi.getInstance().listEnvironments(getApiToken(credentialsId), IdAndName.parse(applicationIdAndName).getId())
                    .forEach(environment -> items.add(environment.getName(), new IdAndName(environment.getKey(), environment.getName()).toString()));

            return items;
            } catch (Exception e) {
                return null;
            }
        }

    }
}
