/*
 * Copyright 2021 CloudBees, Inc.
 * All rights reserved.
 */

package com.cloudbees.fm.jenkins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * 
 */
public class FeatureManagementConfigurationBuilder extends Builder implements SimpleBuildStep {

    private static final Logger LOGGER = Logger.getLogger(FeatureManagementConfigurationBuilder.class.getName());

    private final String applicationId;
    private String environmentName;
    private final String flagConfigInstructions;

    @DataBoundConstructor
    public FeatureManagementConfigurationBuilder(String applicationId, String flagConfigInstructions) {
        this.applicationId = applicationId;
        this.flagConfigInstructions = flagConfigInstructions;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    @DataBoundSetter
    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getFlagConfigInstructions() {
        return flagConfigInstructions;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env,
                        @NonNull Launcher launcher, @NonNull TaskListener listener)
            throws InterruptedException, IOException {
        Optional<StringCredentials> accessToken = FeatureManagementGlobalConfiguration.get().getAccessTokenCredential();
        
        if (!accessToken.isPresent()) {
            LOGGER.warning("'accessTokenCredentialId' is not configured in the System Configuration.");
            return;
        }
        
        // TODO: Add support for variable substitutions to allow a more dynamic config of flags?
        
        JSON json = JSONSerializer.toJSON(flagConfigInstructions);
        
        if (!(json instanceof JSONObject)) {
            LOGGER.warning("'flagConfigInstructions' is not a value JSON Object.");
            return;
        }

        JSONObject config = (JSONObject) json;
        JSONArray setArray = config.optJSONArray("set");
        JSONArray deleteArray = config.optJSONArray("delete");
        FeatureManagementAPI api = new FeatureManagementAPI(accessToken.get());

        if (setArray != null) {
            doSets(api, setArray);
        }
        if (deleteArray != null) {
            doDeletes(api, deleteArray);
        }
    }

    private void doSets(FeatureManagementAPI api, JSONArray setArray) {
        try {
            for (int i = 0; i < setArray.size(); i++) {
                JSONObject flagConfig = setArray.optJSONObject(i);
                
                if (flagConfig != null) {
                    api.setFlag(this.applicationId, this.environmentName, flagConfig);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error processing Feature Management configuration set.", e);
        }
    }

    private void doDeletes(FeatureManagementAPI api, JSONArray deleteArray) {
        try {
            for (int i = 0; i < deleteArray.size(); i++) {
                String flagName = deleteArray.optString(i);

                if (flagName != null) {
                    api.deleteFlag(this.applicationId, flagName);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error processing Feature Management flag deletion set.", e);
        }
    }

    @Symbol("featureManagementConfig")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public String getDisplayName() {
            return "Feature Management Configuration";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
