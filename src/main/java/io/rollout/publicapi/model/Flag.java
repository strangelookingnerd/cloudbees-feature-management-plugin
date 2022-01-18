package io.rollout.publicapi.model;

/**
 * At this point, we really don't care about the structure of a flag. We can just treat it as a generic map
 */
public class Flag extends ConfigEntity {

    public boolean isEnabled() {
        return (boolean) get("enabled");
    }
}
