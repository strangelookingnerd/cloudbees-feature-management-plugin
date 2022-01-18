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

package io.rollout.publicapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rollout.publicapi.model.Application;
import io.rollout.publicapi.model.AuditLog;
import io.rollout.publicapi.model.Environment;
import io.rollout.publicapi.model.Flag;
import io.rollout.publicapi.model.TargetGroup;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.httpclient.HttpException;

/**
 * Class for interacting with the Rollout Public API: https://docs.cloudbees.com/docs/cloudbees-feature-management-rest-api
 */
public class PublicApi {
    private static final String API_URL = "https://x-api.rollout.io/public-api";
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
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
            throw new HttpException(String.format("%d error performing GET on %s: %s", response.code(), url, response.body().string()));
        }
    }

    public List<Application> listApplications(String accessToken) throws IOException {
        return get(HttpUrl.parse(API_URL + "/applications"), accessToken, new TypeReference<List<Application>>(){});
    }

    public List<Environment> listEnvironments(String accessToken, String applicationId) throws IOException {
        return get(HttpUrl.parse(API_URL + "/applications/" + applicationId + "/environments"), accessToken, new TypeReference<List<Environment>>(){});
    }

    public List<Flag> getFlags(String accessToken, String applicationId, String environmentName) throws IOException {
        return get(HttpUrl.parse(API_URL + "/applications/" + applicationId + "/" + environmentName + "/flags"), accessToken, new TypeReference<List<Flag>>(){});
    }

    public List<TargetGroup> getTargetGroups(String accessToken, String applicationId) throws IOException {
        return get(HttpUrl.parse(API_URL + "/applications/" + applicationId + "/target-groups"), accessToken, new TypeReference<List<TargetGroup>>(){});
    }

    public List<AuditLog> getAuditLogs(String accessToken, String applicationId, String environmentName, Date startDate) throws IOException {
        // TODO. The public API automatically paginates the response (30 items max). 😢
        HttpUrl url = HttpUrl
                .parse(API_URL + "/applications/" + applicationId + "/" + environmentName + "/auditlogs")
                .newBuilder()
                .addQueryParameter("startDate", startDate.toInstant().toString())
                .build();
        return get(url, accessToken, new TypeReference<List<AuditLog>>(){});
    }
}
