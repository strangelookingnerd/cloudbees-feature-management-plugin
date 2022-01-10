package io.rollout.configuration.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import hudson.model.Run;
import io.rollout.configuration.Configuration;
import io.rollout.configuration.LocalConfiguration;
import io.rollout.configuration.json.ExperimentModelDeserializer;
import io.rollout.flags.models.ExperimentModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * The thing that's responsible for loading/saving {@link Configuration} objects to disk. It'll store them in the getRoot
 * directory of the specified {@link Run} with a filename that inculdes the environment ID.
 */
public class ConfigurationPersister {
    private final transient ObjectMapper mapper;
    private static ConfigurationPersister instance = new ConfigurationPersister();

    private ConfigurationPersister() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ExperimentModel.class, new ExperimentModelDeserializer());
        mapper.registerModule(module);
    }

    private static String generateFileName(String environmentId) {
        return "cbfm-configuration-" + environmentId + ".json";
    }

    private static File configFile(Run<?, ?> run, String environmentId) {
        return Paths.get(run.getRootDir() + "/" + generateFileName(environmentId)).toFile();
    }

    public static ConfigurationPersister getInstance() {
        return instance;
    }

    public void save(Configuration configuration, Run<?, ?> run, String environmentId) throws IOException {
        mapper.writeValue(configFile(run, environmentId), configuration);
    }

    public Configuration load(Run<?, ?> run, String environmentId) throws IOException {
        return mapper.readValue(configFile(run, environmentId), LocalConfiguration.class);
    }
}
