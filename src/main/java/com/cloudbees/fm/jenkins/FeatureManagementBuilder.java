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
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class FeatureManagementBuilder extends Builder implements SimpleBuildStep {
    
    private final String flagInstructionsFilePath;

    @DataBoundConstructor
    public FeatureManagementBuilder(String flagInstructionsFilePath) {
        this.flagInstructionsFilePath = flagInstructionsFilePath;
    }

    public String getFlagInstructionsFilePath() {
        return flagInstructionsFilePath;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env,
                        @NonNull Launcher launcher, @NonNull TaskListener listener)
            throws InterruptedException, IOException {
        listener.getLogger().println("*** flagInstructionsFilePath: " + flagInstructionsFilePath);
    }

    @Symbol("featureManagement")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public String getDisplayName() {
            return "Feature Management";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
