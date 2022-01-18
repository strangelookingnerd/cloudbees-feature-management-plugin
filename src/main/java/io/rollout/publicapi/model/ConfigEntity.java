package io.rollout.publicapi.model;

import java.util.HashMap;

public abstract class ConfigEntity extends HashMap<String, Object> {
    public String getName() {
        return get("name").toString();
    }
}
