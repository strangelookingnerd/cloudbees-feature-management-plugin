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

package io.rollout.publicapi.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.commons.lang3.ObjectUtils;

public class DataPersister {
    public enum EntityType {
        FLAG,
        TARGET_GROUP,
        AUDIT_LOG
    }

    private DataPersister() {
        throw new RuntimeException("Utility class. Do not instantiate");
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void writeValue(File dir, String environmentId, EntityType entityType, Object value) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(filename(dir, environmentId, entityType), value);
    }

    public static <T> T readValue(File dir, String environmentId, EntityType entityType, TypeReference<T> typeReference, T defaultValue) throws IOException {
        final File file = filename(dir, environmentId, entityType);

        if (!file.exists()) {
            return defaultValue;
        }

        return mapper.readValue(file, typeReference);
    }

    public static File filename(File dir, String environmentId, EntityType entityType) {
        ObjectUtils.requireNonEmpty(environmentId);
        ObjectUtils.requireNonEmpty(entityType);
        return Paths.get(dir.getAbsolutePath(), environmentId + "-" + entityType + ".json").toFile();
    }
}
