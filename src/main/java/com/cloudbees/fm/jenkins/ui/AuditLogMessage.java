package com.cloudbees.fm.jenkins.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A parsed/formatted version of an Audit Log message, ready for rendering in the UI
 */
public class AuditLogMessage {
    private static final String TERM = "<audit_log_targeting_term>";
    private static final String DELIMITER = "<audit_log_targeting_delimiter>";

    private final String mainMessage;

    private final List<String> subItems;

    public AuditLogMessage(String message) {
        String[] parts = message.split(TERM);
        mainMessage = parts[0];
        if (parts.length > 1) {
            subItems = Arrays.asList(parts[1].split(DELIMITER));
        } else {
            subItems = Collections.emptyList();
        }
    }

    public String getMainMessage() {
        return mainMessage;
    }

    public List<String> getSubItems() {
        return subItems;
    }
}
