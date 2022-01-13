package io.rollout.configuration.comparison;

import com.google.common.annotations.VisibleForTesting;
import io.rollout.configuration.Configuration;
import io.rollout.flags.models.ExperimentModel;
import io.rollout.flags.models.TargetGroupModel;
import io.rollout.publicapi.model.Flag;
import io.rollout.publicapi.model.TargetGroup;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A class that will compare two different {@link Configuration}s. It will return a {@link ComparisonResult} that will supplied configurations into the following groups:
 * <ul>
 *     <li>Model elements that are present in the first {@link Configuration} only</li>
 *     <li>Model elements that are present in the second {@link Configuration} only</li>
 *     <li>Model elements that are present in the both {@link Configuration}s and the same in both Configurations</li>
 *     <li>Model elements that are present in the both {@link Configuration}s but the Configurations differ (but with the same ID)</li>
 * </ul>
 * When comparing the {@link ExperimentModel} and {@link TargetGroupModel} models, we use the ID field to determine whether it has changed between one {@link Configuration} and
 * the other
 */
public class ConfigurationComparator {
    public ConfigurationComparisonResult compare(Configuration first, Configuration second) {
        ComparisonResult<ExperimentModel> experimentModelComparisonResult = compare(first.getExperiments(), second.getExperiments());
        ComparisonResult<TargetGroupModel> targetGroupModelComparisonResult = compare(first.getTargetGroups(), second.getTargetGroups());
        return new ConfigurationComparisonResult(experimentModelComparisonResult, targetGroupModelComparisonResult);
    }

    public <T> ComparisonResult<T> compare(List<T> first, List<T> second) {
        ComparisonResult<T> result = new ComparisonResult<>();

        // Work through the experiments first.
        // Create maps of the old config by ID and the new config by ID, and a set of IDs.
        Map<String, T> firstById = first.stream().collect(Collectors.toMap(this::getId, Function.identity()));
        Map<String, T> secondById = second.stream().collect(Collectors.toMap(this::getId, Function.identity()));
        Set<String> ids = new HashSet<>(firstById.keySet()); // make it mutable
        ids.addAll(secondById.keySet());

        ids.forEach(id -> {
            T firstModel = firstById.get(id);
            T secgondModel = secondById.get(id);
            if (firstModel != null && secgondModel != null) {
                if (firstModel.equals(secgondModel)) {
                    result.addInBothAndTheSame(firstModel);
                } else {
                    result.addInBothButDifferent(firstModel, secgondModel);
                }
            } else if (firstModel != null) {
                result.addInFirstOnly(firstModel);
            } else if (secgondModel != null) {
                result.addInSecondOnly(secgondModel);
            } else {
                throw new RuntimeException("Logic fail. We didn't handle experiment " + id);
            }
        });

        return result;
    }

    /**
     * {@link io.rollout.flags.models.ExperimentModel}, {@link io.rollout.flags.models.TargetGroupModel} etc don't inherit any common behaviour for IDs, so just hack it in here
     */
    private String getId(Object model) {
        if (model instanceof ExperimentModel) {
            return ((ExperimentModel) model).getId();
        } else if (model instanceof TargetGroupModel) {
            return ((TargetGroupModel) model).getId();
        } else if (model instanceof Flag) {
            return ((Flag)model).getName();
        } else if (model instanceof TargetGroup) {
            return ((TargetGroup)model).getName();
        } else {
            throw new RuntimeException("Cannot do getId() on " + model.getClass().getSimpleName());
        }
    }

}
