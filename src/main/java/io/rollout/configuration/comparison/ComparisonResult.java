/*
 * The MIT License
 *
 * Copyright 2015-2016 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
