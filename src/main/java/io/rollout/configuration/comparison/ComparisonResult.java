package io.rollout.configuration.comparison;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A class representing the result of a comparison of two different collections of model objects.
 */
public class ComparisonResult<T> {
    private final Set<T> inFirstOnly = new HashSet<>();
    private final Set<T> inSecondOnly = new HashSet<>();
    private final Set<Pair<T, T>> inBothButDifferent = new HashSet<>();
    private final Set<T> inBothAndTheSame = new HashSet<>();

    public Set<T> getInFirstOnly() {
        return inFirstOnly;
    }

    public Set<T> getInSecondOnly() {
        return inSecondOnly;
    }

    public Set<Pair<T, T>> getInBothButDifferent() {
        return inBothButDifferent;
    }

    public Set<T> getInBothAndTheSame() {
        return inBothAndTheSame;
    }

    public boolean areEqual() {
        return inFirstOnly.isEmpty() &&
                inSecondOnly.isEmpty() &&
                inBothButDifferent.isEmpty();
    }

    public void addInFirstOnly(T entity) {
        inFirstOnly.add(entity);
    }

    public void addInSecondOnly(T entity) {
        inSecondOnly.add(entity);
    }

    public void addInBothButDifferent(T first, T second) {
        inBothButDifferent.add(new ImmutablePair<>(first, second));
    }

    public void addInBothAndTheSame(T entity) {
        inBothAndTheSame.add(entity);
    }
}
