package io.rollout.configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

/**
 * Class responsible for downloading the Embedded Configuration from x-api.rollout.io
 */
public class ConfigurationFetcher {
    private final OkHttpClient client;
    private final static ConfigurationFetcher INSTANCE = new ConfigurationFetcher();

    public static ConfigurationFetcher getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a ConfigurationFetcher with the default {@link OkHttpClient}
     */
    public ConfigurationFetcher() {
        this(new OkHttpClient.Builder().build());
    }

    /**
     * Creates a ConfigurationFetcher with the specified {@link OkHttpClient}
     * @param client the {@link OkHttpClient} to use
     */
    public ConfigurationFetcher(OkHttpClient client) {
        this.client = client;
    }

    private Response downloadConfiguration(String environmentId) throws IOException {
        if (StringUtils.isBlank(environmentId)) {
            throw new IllegalArgumentException("environmentId is required");
        }

        // Make the HTTP call to get the embedded configuration
        HttpUrl url = HttpUrl.parse("https://x-api.rollout.io/device/embedded_configuration").newBuilder()
                .addQueryParameter("app_key", environmentId) // app_key is the old name, but it's the environmentId in reality
                .addQueryParameter("api_version", "1.9.0")
                .addQueryParameter("sdk_version", "5.1.2")
                .addQueryParameter("platform", "default")
                .build();
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();

        return client.newCall(request).execute();
    }

    public Configuration getConfiguration(String environmentId) throws IOException, ParseException {
        ConfigurationFactory factory = new ConfigurationFactory(environmentId);

        Response response = downloadConfiguration(environmentId);
        if (response.code() == 200) {
            return factory.build(io.rollout.networking.Response.createFromNetworkResponse(response, false));
        } else {
            throw new FileNotFoundException("Couldn't get configuration for environmentId " + environmentId + ". Status: " + response.code());
        }
    }

}
