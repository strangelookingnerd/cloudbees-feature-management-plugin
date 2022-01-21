/*
 * The MIT License
 *
 * Copyright 2015-2016 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.security.Permission;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.rollout.publicapi.PublicApi;
import io.rollout.publicapi.model.Application;
import io.rollout.publicapi.model.AuditLog;
import io.rollout.publicapi.model.DataPersister;
import io.rollout.publicapi.model.Environment;
import io.rollout.publicapi.model.Flag;
import io.rollout.publicapi.model.TargetGroup;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
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
import org.kohsuke.stapler.verb.POST;

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

        try {
            String apiToken = ((DescriptorImpl) getDescriptor()).getApiToken(credentialsId);
            downloadAndSaveFlags(apiToken, run, listener);
            if (run.getPreviousSuccessfulBuild() != null) {
                // Audit logs (to show changes) are only relevant when comparing against a previous (successful) build.
                downloadAndSaveAuditLogs(apiToken, run, listener, run.getPreviousSuccessfulBuild().getTime());
            }
            run.addAction(new FeatureManagementConfigurationAction(application, environment));
        } catch (Exception e) {
            listener.getLogger().printf("Error fetching flag configurations: %s\n", e);
            run.setResult(Result.UNSTABLE);
        }
    }

    private void downloadAndSaveFlags(String apiToken, Run<?,?> run, TaskListener listener) throws IOException {
        // Download and save the flags and target groups from the public API
        List<Flag> flags = PublicApi.getInstance().getFlags(apiToken, application.getId(), environment.getName());
        List<TargetGroup> targetGroups = PublicApi.getInstance().getTargetGroups(apiToken, application.getId());

        // Save the flags and target groups to disk
        DataPersister.writeValue(run.getRootDir(), environment.getKey(), DataPersister.EntityType.FLAG, flags);
        DataPersister.writeValue(run.getRootDir(), environment.getKey(), DataPersister.EntityType.TARGET_GROUP, targetGroups);

        listener.getLogger().printf("For %s/%s there are %d flags (%d enabled) and %d target groups\n", application.getName(), environment.getName(), flags.size(), flags.stream().filter(Flag::isEnabled).count(), targetGroups.size());
    }

    private void downloadAndSaveAuditLogs(String apiToken, Run<?,?> run, TaskListener listener, Date startDate) throws IOException {
        List<AuditLog> auditLogs = PublicApi.getInstance().getAuditLogs(apiToken, application.getId(), environment.getName(), startDate);
        DataPersister.writeValue(run.getRootDir(), environment.getKey(), DataPersister.EntityType.AUDIT_LOG, auditLogs);
        listener.getLogger().printf("For %s/%s there were %d changes from the audit logs\n", application.getName(), environment.getName(), auditLogs.size());
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

        @POST
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

        @POST
        public FormValidation doCheckCredentialsId(@QueryParameter String credentialsId, @AncestorInPath Item item) {
            // Checking that the user has permissions to configure this item prevents unauthenticated users trying to brute-force credential IDs
            // https://www.jenkins.io/doc/developer/security/form-validation/
            if (item == null) { // no context
                return FormValidation.ok();
            }
            item.checkPermission(Item.CONFIGURE);

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

        @POST
        public ListBoxModel doFillApplicationIdAndNameItems(@QueryParameter String credentialsId, @AncestorInPath Item item) throws IOException {
            if (item != null) {
                if (!item.hasPermission(Permission.CONFIGURE)) {
                    return new StandardListBoxModel().includeEmptyValue();
                }
            }
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

        @POST
        public ListBoxModel doFillEnvironmentIdAndNameItems(@QueryParameter String credentialsId, @QueryParameter String applicationIdAndName, @AncestorInPath Item item) throws IOException {
            if (item != null) {
                if (!item.hasPermission(Permission.CONFIGURE)) {
                    return new StandardListBoxModel().includeEmptyValue();
                }
            }
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
