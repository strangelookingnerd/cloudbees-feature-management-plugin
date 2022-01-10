package com.cloudbees.fm.jenkins;

import hudson.model.Run;
import io.rollout.configuration.Configuration;
import io.rollout.configuration.comparison.ConfigurationComparator;
import io.rollout.configuration.persistence.ConfigurationPersister;
import io.rollout.flags.models.ExperimentModel;
import io.rollout.flags.models.TargetGroupModel;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;
import java.util.List;
import jenkins.model.RunAction2;

public class FeatureManagementConfigurationAction implements RunAction2 {

    private final String environmentId;
    private transient Run<?, ?> run;
    private transient Configuration configuration; // lazy loaded. Do not use this directly. Use the getter instead.

    FeatureManagementConfigurationAction(String environmentId) {
        this.environmentId = environmentId;
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

    public String getEnvironmentId() {
        return environmentId;
    }

    private Configuration getConfiguration() throws IOException {
        if (configuration == null) {
            configuration = ConfigurationPersister.getInstance().load(run, environmentId);
        }

        return configuration;
    }

    public Collection<ExperimentModel> getExperiments() throws IOException {
        return getConfiguration().getExperiments();
    }

    public List<TargetGroupModel> getTargetGroups() throws IOException {
        return getConfiguration().getTargetGroups();
    }

    public String getSignedDate() {
        // TODO - how do we get the user's timezone when rendering server-side data?
        DateTimeFormatter formatter =
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
//                        .withLocale(Locale.UK)
                        .withZone(ZoneId.systemDefault());
        return formatter.format(Instant.now());
    }

    public Run<?, ?> getPreviousSuccessfulBuild() {
        return run.getPreviousSuccessfulBuild();
    }

    public boolean getIsUnchanged() throws IOException {
        // Get the configuration from the previous build.
        // TODO - we can probably cache this and move the fetching from the Action to the Run
        Configuration previousConfig = ConfigurationPersister.getInstance().load(run, environmentId);
        return new ConfigurationComparator().compare(previousConfig, getConfiguration()).areEqual();
    }
}
