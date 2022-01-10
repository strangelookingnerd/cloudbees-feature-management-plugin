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
import io.rollout.configuration.Configuration;
import io.rollout.configuration.ConfigurationFetcher;
import io.rollout.configuration.comparison.ConfigurationComparator;
import io.rollout.configuration.comparison.ConfigurationComparisonResult;
import io.rollout.configuration.persistence.ConfigurationPersister;
import java.io.IOException;
import java.text.ParseException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * 
 */
public class FeatureManagementConfigurationBuilder extends Builder implements SimpleBuildStep {

    private String environmentId;

    @DataBoundConstructor
    public FeatureManagementConfigurationBuilder(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    @DataBoundSetter
    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env,
                        @NonNull Launcher launcher, @NonNull TaskListener listener)
            throws InterruptedException, IOException {

        try {
            Configuration config = ConfigurationFetcher.getInstance().getConfiguration(environmentId);
            run.addAction(new FeatureManagementConfigurationAction(environmentId));

            listener.getLogger().printf("Retrieved CloudBees Feature Management configuration for %s. %d Experiments, %d Target Groups. Last Updated: %s\n",
                    environmentId, config.getExperiments().size(), config.getTargetGroups().size(), config.getSignedDate().toString());

            // Save the config
            ConfigurationPersister.getInstance().save(config, run, environmentId);

            // Awesome, we saved the config. Now load the config from the last successful build
            Run<?, ?> previousSuccessfulBuild = run.getPreviousSuccessfulBuild();
            if (previousSuccessfulBuild != null) {
                // read the file
                try {
                    Configuration oldConfig = ConfigurationPersister.getInstance().load(previousSuccessfulBuild, environmentId);

                    ConfigurationComparisonResult comparison = new ConfigurationComparator().compare(oldConfig, config);
                    listener.getLogger().println("configs are " + (comparison.areEqual() ? "not " : "") + "different");

                } catch (Exception e) {
                    listener.getLogger().printf("Could not load previous flag configuration from last successful build (%d)\n", previousSuccessfulBuild.getNumber());
                }
            } else {
                listener.getLogger().println("There were no previous successful build to compare the flag configurations to");
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean requiresWorkspace() {
        return false; // Jesse said this was important
    }

    @Symbol("featureManagementConfig")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        @NonNull
        public String getDisplayName() {
            return "CloudBees Feature Management Configuration";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
