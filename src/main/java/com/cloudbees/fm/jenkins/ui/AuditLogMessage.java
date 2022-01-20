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

package com.cloudbees.fm.jenkins.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A parsed/formatted version of an Audit Log message, ready for rendering in the UI
 */
public class AuditLogMessage {
    private static final String TARGETING_TERM = "<audit_log_targeting_term>";
    private static final String TARGETING_DELIMITER = "<audit_log_targeting_delimiter>";
    private static final String MULTI_TERM = "<audit_log_multi_term>";
    private static final String MULTI_DELIMITER = "<audit_log_multi_delimiter>";
    private static final String MULTI_TERM_END = "<audit_log_multi_term_end>";

    private final String mainMessage;

    private final List<String> subItems;

    public AuditLogMessage(String message) {
        // Total hack - for the moment, treat the two different types of terms and delimiters the same
        message = message.replace(MULTI_TERM, TARGETING_TERM).replace(MULTI_DELIMITER, TARGETING_DELIMITER).replace(MULTI_TERM_END, "");

        String[] parts = message.split(TARGETING_TERM);
        mainMessage = parts[0];
        if (parts.length > 1) {
            subItems = Arrays.asList(parts[1].split(TARGETING_DELIMITER));
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
