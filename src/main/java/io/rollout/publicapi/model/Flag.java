package io.rollout.publicapi.model;

import java.util.HashMap;

/**
 * At this point, we really don't care about the structure of a flag. We can just treat it as a generic map
 */
public class Flag extends HashMap<String, Object> {
    public String getName() {
        return get("name").toString();
    }
}
