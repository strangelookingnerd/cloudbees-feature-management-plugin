package com.cloudbees.fm.jenkins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Run;
import io.rollout.configuration.Configuration;
import io.rollout.configuration.comparison.ConfigurationComparator;
import io.rollout.configuration.comparison.ConfigurationComparisonResult;
import io.rollout.configuration.persistence.ConfigurationPersister;
import io.rollout.flags.models.ExperimentModel;
import io.rollout.flags.models.TargetGroupModel;
import io.rollout.publicapi.model.Application;
import io.rollout.publicapi.model.Environment;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import jenkins.model.RunAction2;

public class FeatureManagementConfigurationAction implements RunAction2 {

    private final Application application;
    private final Environment environment;
    private transient Run<?, ?> run;
    private transient Configuration configuration; // lazy loaded. Do not use this directly. Use the getter instead.

    FeatureManagementConfigurationAction(Application application, Environment environment) {
        this.application = application;
        this.environment = environment;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/cloudbees-feature-management/images/cloudbees.svg";
    }

    @Override
    public String getDisplayName() {
        return "Flag configurations";
    }

    @Override
    public String getUrlName() {
        return "flags";
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        run = r;
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        run = r;
    }

    public Run<?, ?> getOwner() {
        return run;
    }

    public Application getApplication() {
        return application;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public String getRawConfiguration() throws IOException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(getConfiguration());
    }

    public String toJson(Object o) throws JsonProcessingException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(o);
    }

    private Configuration getConfiguration() throws IOException {
        if (configuration == null) {
            configuration = ConfigurationPersister.getInstance().load(run, environment.getKey());
        }

        return configuration;
    }

    public Collection<ExperimentModel> getExperiments() throws IOException {
        return getConfiguration().getExperiments();
    }

    public List<TargetGroupModel> getTargetGroups() throws IOException {
        return getConfiguration().getTargetGroups();
    }

    public Date getSignedDate() throws IOException {
        return getConfiguration().getSignedDate();
    }

    public Run<?, ?> getPreviousSuccessfulBuild() {
        return run.getPreviousSuccessfulBuild();
    }

    public boolean getIsUnchanged() throws IOException {
        return new ConfigurationComparator().compare(getPreviousSuccessfulConfig(), getConfiguration()).areEqual();
    }

    public ConfigurationComparisonResult getConfigurationChanges() throws IOException {
        return new ConfigurationComparator().compare(getPreviousSuccessfulConfig(), getConfiguration());
    }

    private Configuration getPreviousSuccessfulConfig() throws IOException {
        // TODO - we can probably cache this and move the fetching from the Action to the Run
        return ConfigurationPersister.getInstance().load(run.getPreviousSuccessfulBuild(), environment.getKey());
    }
}
