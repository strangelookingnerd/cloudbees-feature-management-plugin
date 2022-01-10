package io.rollout.configuration.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.rollout.flags.models.DeploymentConfiguration;
import io.rollout.flags.models.ExperimentModel;
import io.rollout.flags.models.FeatureFlagModel;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Deserialization of the {@link ExperimentModel} class is annoying. The class has `isArchived` and `isSticky` properties. When serializing, these get turned into
 * `archived` and `sticky` fields in the JSON. However, deserializing (with jackson, org.json doesn't have a handy deserializer unfortunately) these _don't_ get put back
 * into the `isArchived` and `isSticky` class members (jackson I think encourages the use of `hasArchived` field names etc.
 *
 * Changing the class in the SDK is risky, especially for support of a PoC, so let's just use a custom deserializer here instead.
 */
public class ExperimentModelDeserializer extends StdDeserializer<ExperimentModel> {
    public ExperimentModelDeserializer() {
        this(null);
    }

    public ExperimentModelDeserializer(Class<?> clazz) {
        super(clazz);
    }

    @Override
    public ExperimentModel deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        String name = node.get("name").asText();
        DeploymentConfiguration deploymentConfiguration = deserializationContext.readTreeAsValue(node.get("deploymentConfiguration"), DeploymentConfiguration.class);

        ArrayNode flagsNode = (ArrayNode) node.get("featureFlags");
        List<FeatureFlagModel> featureFlags = StreamSupport.stream(flagsNode.spliterator(), false)
                .map(n -> {
                    try {
                        return deserializationContext.readTreeAsValue(n, FeatureFlagModel.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());


        String id = node.get("id").asText();
        boolean isArchived = node.get("archived").asBoolean(); // this is the problematic step
        boolean isSticky = node.get("sticky").asBoolean(); // this one too
        Set<String> labels = deserializationContext.readTreeAsValue(node.get("labels"), Set.class);
        String stickinessProperty = node.get("stickinessProperty").asText();
        return new ExperimentModel(name, deploymentConfiguration, featureFlags, id, isArchived, isSticky, labels, stickinessProperty);
    }
}
