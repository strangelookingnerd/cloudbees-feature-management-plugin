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
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class FeatureManagementConfigurationBuilder extends Builder implements SimpleBuildStep {
    
    private final String flagConfigInstructions;

    @DataBoundConstructor
    public FeatureManagementConfigurationBuilder(String flagConfigInstructions) {
        this.flagConfigInstructions = flagConfigInstructions;
    }

    public String getFlagConfigInstructions() {
        return flagConfigInstructions;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env,
                        @NonNull Launcher launcher, @NonNull TaskListener listener)
            throws InterruptedException, IOException {
        Optional<StringCredentials> accessToken = FeatureManagementGlobalConfiguration.get().getAccessTokenCredential();
        listener.getLogger().println("*** flagConfigInstructions: " + flagConfigInstructions);
        listener.getLogger().println("*** accessToken: " + accessToken);
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
