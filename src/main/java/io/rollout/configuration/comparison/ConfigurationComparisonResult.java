package io.rollout.configuration.comparison;

import io.rollout.configuration.Configuration;
import io.rollout.flags.models.ExperimentModel;
import io.rollout.flags.models.TargetGroupModel;

/**
 * A class representing the result of a comparison of two different {@link Configuration} objects.
 */
public class ConfigurationComparisonResult {
    private final ComparisonResult<ExperimentModel> experimentComparison;
    private final ComparisonResult<TargetGroupModel> targetGroupComparison;

    public ConfigurationComparisonResult(ComparisonResult<ExperimentModel> experimentComparison, ComparisonResult<TargetGroupModel> targetGroupComparison) {
        this.experimentComparison = experimentComparison;
        this.targetGroupComparison = targetGroupComparison;
    }

    public ComparisonResult<ExperimentModel> getExperimentComparison() {
        return experimentComparison;
    }

    public ComparisonResult<TargetGroupModel> getTargetGroupComparison() {
        return targetGroupComparison;
    }

    public boolean areEqual() {
        return experimentComparison.areEqual() && targetGroupComparison.areEqual();
    }
}
