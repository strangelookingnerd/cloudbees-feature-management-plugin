package io.rollout.publicapi.model;

import java.util.Objects;

public class Environment {
    String name;
    String key; // (id)
    String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Environment)) {
            return false;
        }
        Environment that = (Environment) o;
        return Objects.equals(name, that.name) && Objects.equals(key, that.key) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, key, description);
    }

    @Override
    public String toString() {
        return "Environment{" +
                "name='" + name + '\'' +
                ", key='" + key + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
