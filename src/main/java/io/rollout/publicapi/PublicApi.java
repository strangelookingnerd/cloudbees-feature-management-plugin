package io.rollout.publicapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rollout.publicapi.model.Application;
import io.rollout.publicapi.model.Environment;
import java.io.IOException;
import java.util.List;
import io.rollout.okhttp3.HttpUrl;
import io.rollout.okhttp3.OkHttpClient;
import io.rollout.okhttp3.Request;
import io.rollout.okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Class for interacting with the Rollout Public API: https://docs.cloudbees.com/docs/cloudbees-feature-management-rest-api
 */
public class PublicApi {
    private static PublicApi instance;

    private ObjectMapper mapper;
    private final OkHttpClient client;

    /**
     * Creates a ConfigurationFetcher with the default {@link OkHttpClient}
     */
    public PublicApi() {
        this(new OkHttpClient.Builder().build());
    }

    public PublicApi(OkHttpClient client) {
        this.client = client;
        mapper = new ObjectMapper();
    }

    public static PublicApi getInstance() {
        if (instance == null) {
            instance = new PublicApi();
        }

        return instance;
    }

    private <T> T get(HttpUrl url, String accessToken, TypeReference<T> typeReference) throws IOException {
        Request request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            return mapper.readValue(response.body().string(), typeReference);
        } else {
            throw new HttpClientErrorException(HttpStatus.valueOf(response.code()), response.body().string());
        }
    }

    public List<Application> listApplications(String accessToken) throws IOException {
        return get(HttpUrl.parse("https://x-api.rollout.io/public-api/applications"), accessToken, new TypeReference<List<Application>>(){});
    }

    public List<Environment> listEnvironments(String accessToken, String applicationId) throws IOException {
        return get(HttpUrl.parse("https://x-api.rollout.io/public-api/applications/" + applicationId + "/environments"), accessToken, new TypeReference<List<Environment>>(){});
    }

}
