package io.rollout.configuration.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.rollout.flags.models.DeploymentConfiguration;
import io.rollout.flags.models.ExperimentModel;
import io.rollout.flags.models.FeatureFlagModel;
import junit.framework.TestCase;
import org.junit.Test;

public class ExperimentModelDeserializerTest extends TestCase {
    @Test
    public void testSerializeDeserialize() throws JsonProcessingException {
        ExperimentModel model = new ExperimentModel(
                "myname",
                new DeploymentConfiguration("mycondit"),
                ImmutableList.of(new FeatureFlagModel("myflag", true)),
                "myid",
                true, true,
                ImmutableSet.of("l1", "l2"),
                "mystick");

        ObjectMapper mapper = new ObjectMapper();
        // Register the custom deserializer
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ExperimentModel.class, new ExperimentModelDeserializer());
        mapper.registerModule(module);

        String json = mapper.writeValueAsString(model);
        ExperimentModel deserializedModel = mapper.readValue(json, ExperimentModel.class);

        assertEquals(model, deserializedModel);
    }
}
