package com.cloudbees.fm.jenkins;

/**
 * A quick utility class to pass an ID and a name through jelly forms when selecting an option (and you are forced to use a string as the value)
 */
public class IdAndName {
    // This is probably not ideal, because on the off-chance a customer has an app/env with a name that contains ::, it'll break. But it's probably good enough for now
    private static final String DELIMITER = "::";
    private final String id;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    private final String name;

    public IdAndName(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static IdAndName parse(String idAndName) {
        String[] parts = idAndName.split(DELIMITER);
        if (parts.length == 2) {
            return new IdAndName(parts[0], parts[1]);
        } else {
            throw new IllegalArgumentException("Cannot parse " + idAndName);
        }
    }

    @Override
    public String toString() {
        return id + DELIMITER + name;
    }
}
