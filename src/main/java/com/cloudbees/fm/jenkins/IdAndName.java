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

package com.cloudbees.fm.jenkins;

/**
 * A quick utility class to pass an ID and a name through jelly forms when selecting an option (and you are forced to use a string as the value)
 */
public class IdAndName {
    // This is probably not ideal, because on the off-chance a customer has an app/env with a name that contains ::, it'll break. But it's probably good enough for now
    private static final String DELIMITER = "::";
    private final String id;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    private final String name;

    public IdAndName(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static IdAndName parse(String idAndName) {
        String[] parts = idAndName.split(DELIMITER);
        if (parts.length == 2) {
            return new IdAndName(parts[0], parts[1]);
        } else {
            throw new IllegalArgumentException("Cannot parse " + idAndName);
        }
    }

    @Override
    public String toString() {
        return id + DELIMITER + name;
    }
}
