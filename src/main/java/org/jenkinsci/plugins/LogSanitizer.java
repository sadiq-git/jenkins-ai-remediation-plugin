// File: src/main/java/org/jenkinsci/plugins/LogSanitizer.java
package org.jenkinsci.plugins;

import java.util.regex.Pattern;

public class LogSanitizer {
    
    private static final Pattern[] SECRETS_PATTERNS = {
        Pattern.compile("(?i)(ghp_[a-zA-Z0-9]{36}|github_pat_[a-zA-Z0-9_]{82})"),
        Pattern.compile("(?i)(AKIA[0-9A-Z]{16})"),
        Pattern.compile("(?i)(eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+)")
    };

    public static String sanitize(String rawLog) {
        if (rawLog == null) return "";
        String safeLog = rawLog;
        for (Pattern pattern : SECRETS_PATTERNS) {
            safeLog = pattern.matcher(safeLog).replaceAll("<REDACTED_SECRET>");
        }
        return safeLog;
    }
}