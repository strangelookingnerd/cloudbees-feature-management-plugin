package com.cloudbees.fm.jenkins;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

/**
 * 
 */
public class FeatureManagementAPI {

    private static final String API_URL = "https://x-api.rollout.io/public-api";
    private static final int TIMEOUT = 60_000;
    
    private enum Method {
        PUT,
        DELETE
    }

    private StringCredentials accessToken;

    public FeatureManagementAPI(StringCredentials accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Set (create and/or configure) a Feature Flag.
     * <p/>
     * Creates the flag if needed and/or configures it's properties.
     *
     * @param applicationId The application Id where the flag is to be created/configured.
     * @param environmentName Environment name. Only relevant if the flag is to be configured (and not just created).
     * @param flagConfig The flag configuration used to create/configure. An instance of the
     *                   <a href="https://docs.cloudbees.com/docs/cloudbees-feature-management-rest-api/latest/flags#anchor-flag-request">Flag schema</a>.
     * @return Operation API call response.
     * @throws IOException An error making the REST API call.
     */
    public Response setFlag(String applicationId, String environmentName, JSONObject flagConfig) throws IOException {
        String flagName = flagConfig.optString("name");
        
        if (flagName == null) {
            return new Response(HttpURLConnection.HTTP_BAD_REQUEST).setPayload("Missing flag 'name' property.");
        }

        // Ensure that the flag exists ...
        Response response = execute(
                Method.PUT,
                // https://docs.cloudbees.com/docs/cloudbees-feature-management-rest-api/latest/flags#_create_flag
                String.format(
                        "/applications/%s/flags",
                        URLEncoder.encode(applicationId, "UTF-8")
                ),
                flagConfig
        );
        
        // If there's an environment name specified ...
        if (environmentName != null) {
            response = execute(
                    Method.PUT,
                    // https://docs.cloudbees.com/docs/cloudbees-feature-management-rest-api/latest/flags#_create_a_flag_configuration
                    String.format(
                            "/applications/%s/%s/flags/%s",
                            URLEncoder.encode(applicationId, "UTF-8"),
                            URLEncoder.encode(environmentName, "UTF-8"),
                            URLEncoder.encode(flagName, "UTF-8")
                    ),
                    flagConfig
            );
        }

        return response;
    }

    public Response deleteFlag(String applicationId, String flagName) throws IOException {
        Response response = execute(
                Method.DELETE,
                // https://docs.cloudbees.com/docs/cloudbees-feature-management-rest-api/latest/flags#_delete_flag
                String.format(
                        "/applications/%s/flags/%s",
                        URLEncoder.encode(applicationId, "UTF-8"),
                        URLEncoder.encode(flagName, "UTF-8")
                )
        );
        
        return response;
    }

    private Response execute(Method method, String path) throws IOException {
        return execute(method, path, null);
    }
    
    private Response execute(Method method, String path, JSONObject payload) throws IOException {
        HttpURLConnection connection = newHttpConnection(method, path);
        
        if (payload != null) {
            try (OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream())) {
                out.write(payload.toString());
            }
        }

        int status = connection.getResponseCode();
        InputStream responseStream;
        
        if (status > 299) {
            responseStream = connection.getErrorStream();
        } else {
            responseStream = connection.getInputStream();
        }

        Response response = new Response(status);

        if (responseStream != null) {
            try (Reader responseReader = new InputStreamReader(responseStream)) {
                StringBuilder responseString = new StringBuilder();
                char[] readBuffer = new char[1024];
                int readCount = 0;
                
                while ((readCount = responseReader.read(readBuffer)) > 0) {
                    responseString.append(readBuffer, 0, readCount);
                }

                response.setPayload(responseString.toString());
            }
        }
        
        return response;
    }

    private HttpURLConnection newHttpConnection(Method method, String path) throws IOException {
        URL url = new URL(API_URL + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(method.name());
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        
        connection.setRequestProperty("Authorization", String.format("Bearer %s", this.accessToken.getSecret().getPlainText()));
        
        if (method == Method.PUT) {
            connection.setRequestProperty("Content-Type", "application/json");
        }

        return connection;
    }
    
    class Response {
        
        private final int status;
        private String payload;

        public Response(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }

        public String getPayload() {
            return payload;
        }

        public Response setPayload(String payload) {
            this.payload = payload;
            return this;
        }
    }
}
