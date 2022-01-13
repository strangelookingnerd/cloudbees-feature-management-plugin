package com.cloudbees.fm.jenkins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Run;
import io.rollout.configuration.Configuration;
import io.rollout.configuration.comparison.ComparisonResult;
import io.rollout.configuration.comparison.ConfigurationComparator;
import io.rollout.configuration.comparison.ConfigurationComparisonResult;
import io.rollout.configuration.persistence.ConfigurationPersister;
import io.rollout.flags.models.ExperimentModel;
import io.rollout.flags.models.TargetGroupModel;
import io.rollout.publicapi.model.Application;
import io.rollout.publicapi.model.DataPersister;
import io.rollout.publicapi.model.Environment;
import io.rollout.publicapi.model.Flag;
import io.rollout.publicapi.model.TargetGroup;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import jenkins.model.RunAction2;
import org.apache.commons.io.IOUtils;

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

    public List<Flag> getPublicApiFlags() throws IOException {
        return DataPersister.readValue(run.getRootDir(), environment.getKey(), DataPersister.EntityType.FLAG, new TypeReference<List<Flag>>() {})
                .stream()
//                .filter(Flag::isEnabled)
                .collect(Collectors.toList());
    }

    public List<TargetGroup> getPublicApiTargetGroups() throws IOException {
        return DataPersister.readValue(run.getRootDir(), environment.getKey(), DataPersister.EntityType.TARGET_GROUP, new TypeReference<List<TargetGroup>>() {});
    }

    public String getRawFlags() throws IOException {
        return IOUtils.toString(new FileInputStream(DataPersister.filename(run.getRootDir(), environment.getKey(), DataPersister.EntityType.FLAG)), StandardCharsets.UTF_8);
    }

    public String getRawTargetGroups() throws IOException {
        return IOUtils.toString(new FileInputStream(DataPersister.filename(run.getRootDir(), environment.getKey(), DataPersister.EntityType.TARGET_GROUP)), StandardCharsets.UTF_8);
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

    public ComparisonResult<Flag> getFlagChanges() throws IOException {
        return new ConfigurationComparator().compare(getPublicApiFlags(), getPreviousSuccessfulFlags());
    }

    public ComparisonResult<TargetGroup> getTargetGroupChanges() throws IOException {
        return new ConfigurationComparator().compare(getPublicApiTargetGroups(), getPreviousSuccessfulTargetGroups());
    }

    private List<Flag> getPreviousSuccessfulFlags() throws IOException {
        // TODO - we can probably cache this and move the fetching from the Action to the Run
        return DataPersister.readValue(run.getPreviousSuccessfulBuild().getRootDir(), environment.getKey(), DataPersister.EntityType.FLAG, new TypeReference<List<Flag>>() {});
    }

    private List<TargetGroup> getPreviousSuccessfulTargetGroups() throws IOException {
        // TODO - we can probably cache this and move the fetching from the Action to the Run
        return DataPersister.readValue(run.getPreviousSuccessfulBuild().getRootDir(), environment.getKey(), DataPersister.EntityType.TARGET_GROUP, new TypeReference<List<TargetGroup>>() {});
    }
}
