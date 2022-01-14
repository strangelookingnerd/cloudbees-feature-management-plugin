package com.cloudbees.fm.jenkins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Run;
import io.rollout.configuration.comparison.ComparisonResult;
import io.rollout.configuration.comparison.ConfigurationComparator;
import io.rollout.publicapi.model.Application;
import io.rollout.publicapi.model.DataPersister;
import io.rollout.publicapi.model.Environment;
import io.rollout.publicapi.model.Flag;
import io.rollout.publicapi.model.TargetGroup;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import jenkins.model.Jenkins;
import jenkins.model.RunAction2;
import org.apache.commons.io.IOUtils;

public class FeatureManagementConfigurationAction implements RunAction2 {

    private final Application application;
    private final Environment environment;
    private transient Run<?, ?> run;

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

    public String toJson(Object o) throws JsonProcessingException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(o);
    }

    public List<Flag> getFlags() throws IOException {
        return DataPersister.readValue(run.getRootDir(), environment.getKey(), DataPersister.EntityType.FLAG, new TypeReference<List<Flag>>() {});
    }

    public List<TargetGroup> getTargetGroups() throws IOException {
        return DataPersister.readValue(run.getRootDir(), environment.getKey(), DataPersister.EntityType.TARGET_GROUP, new TypeReference<List<TargetGroup>>() {});
    }

    public String getRawFlags() throws IOException {
        return IOUtils.toString(new FileInputStream(DataPersister.filename(run.getRootDir(), environment.getKey(), DataPersister.EntityType.FLAG)), StandardCharsets.UTF_8);
    }

    public String getRawTargetGroups() throws IOException {
        return IOUtils.toString(new FileInputStream(DataPersister.filename(run.getRootDir(), environment.getKey(), DataPersister.EntityType.TARGET_GROUP)), StandardCharsets.UTF_8);
    }

    public Run<?, ?> getPreviousSuccessfulBuild() {
        return run.getPreviousSuccessfulBuild();
    }

    public boolean getHasChanged() throws IOException {
        return !getFlagChanges().areEqual() || !getTargetGroupChanges().areEqual();
    }

    public ComparisonResult<Flag> getFlagChanges() throws IOException {
        return new ConfigurationComparator().compare(getFlags(), getPreviousSuccessfulFlags());
    }

    public ComparisonResult<TargetGroup> getTargetGroupChanges() throws IOException {
        return new ConfigurationComparator().compare(getTargetGroups(), getPreviousSuccessfulTargetGroups());
    }

    private List<Flag> getPreviousSuccessfulFlags() throws IOException {
        // TODO - we can probably cache this and move the fetching from the Action to the Run
        return DataPersister.readValue(run.getPreviousSuccessfulBuild().getRootDir(), environment.getKey(), DataPersister.EntityType.FLAG, new TypeReference<List<Flag>>() {});
    }

    private List<TargetGroup> getPreviousSuccessfulTargetGroups() throws IOException {
        // TODO - we can probably cache this and move the fetching from the Action to the Run
        return DataPersister.readValue(run.getPreviousSuccessfulBuild().getRootDir(), environment.getKey(), DataPersister.EntityType.TARGET_GROUP, new TypeReference<List<TargetGroup>>() {});
    }

    public String getUrl() {
        return Jenkins.get().getRootUrl() + run.getUrl() + getUrlName() + "/";
    }
}
