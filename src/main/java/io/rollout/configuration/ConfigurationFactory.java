package io.rollout.configuration;

import io.rollout.networking.Response;
import io.rollout.security.SignatureVerifier;
import java.text.ParseException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Copied from https://github.com/rollout/rox-java-core/blob/master/rox-java-core/src/main/java/io/rollout/configuration/ConfigurationFactory.java
 */
public class ConfigurationFactory {
    private final SignatureVerifier signatureVerifier = new SignatureVerifier();
    private final String appKey;

    public ConfigurationFactory(String appKey) {
        this.appKey = appKey;
    }

    public Configuration build(Response response) throws JSONException, SecurityException, ParseException {
        JSONObject jsonObject = response.getJSONObject();
        return build(jsonObject, response.isRoxyMode());
    }

    public Configuration build(JSONObject jsonObject, boolean isRoxy) throws JSONException, SecurityException, ParseException {
        Object dataContainer = jsonObject.get("data");
        Object signature = jsonObject.get("signature_v0");
        Object signatureDate = jsonObject.get("signed_date");

        if (dataContainer instanceof String) {
            if (!(signature instanceof String)) {
                throw new JSONException("Signature is not a string");
            }

            String dataContainerString = dataContainer.toString();

            boolean isVerified;
            try {
                isVerified = isRoxy|| /*roxSettings.getRolloutEnvironment().getIsSelfManaged() ||*/
                        signatureVerifier.verifySigning(dataContainerString, signature.toString());
            } catch (Exception e) {
                throw new SecurityException(e);
            }

            if (!isVerified) {
                throw new SecurityException("Verifying signature failed");
            }

            JSONObject dataObject = new JSONObject(dataContainerString);
            if (!isRoxy && (!dataObject.has("application") || !dataObject.getString("application").equals(/*roxSettings.getRoxKey()*/appKey))) {
                throw new SecurityException("The api key initialized for the sdk does not match the JSON configuration " + /*roxSettings.getRoxKey()*/appKey);
            }

            return getProductionConfiguration(dataObject, signatureDate);
        }
        throw new JSONException("dataContainer is not a string");
    }


    public Configuration getProductionConfiguration(JSONObject jsonObject, Object signatureDate)
            throws JSONException, ParseException {

        return new ConfigurationBuilder().setJsonObject(jsonObject, signatureDate).build();
    }
}
