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

    public static <T> T readValue(File dir, String environmentId, EntityType entityType, TypeReference<T> typeReference) throws IOException {
        return mapper.readValue(filename(dir, environmentId, entityType), typeReference);
    }

    public static File filename(File dir, String environmentId, EntityType entityType) {
        ObjectUtils.requireNonEmpty(environmentId);
        ObjectUtils.requireNonEmpty(entityType);
        return Paths.get(dir.getAbsolutePath(), environmentId + "-" + entityType + ".json").toFile();
    }
}
