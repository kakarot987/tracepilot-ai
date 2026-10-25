package com.tracepilot.security;

import java.util.regex.Pattern;

/**
 * Redacts common secrets/PII before text is supplied to a model.
 * Best-effort defense in depth; not a replacement for source-side controls.
 */
public final class PromptRedactor {
    private static final Pattern BEARER = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+/-]+=*");
    private static final Pattern ASSIGNMENT = Pattern.compile(
            "(?i)\\b(api[_-]?key|access[_-]?token|refresh[_-]?token|password|passwd|secret)\\b(\\s*[:=]\\s*)[\\x22']?[^\\s,;\\x22']+");
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern IPV4 = Pattern.compile(
            "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    private PromptRedactor() {}

    public static String redact(String text) {
        if (text == null) {
            return null;
        }
        String result = BEARER.matcher(text).replaceAll("Bearer [REDACTED]");
        result = ASSIGNMENT.matcher(result).replaceAll("$1$2[REDACTED]");
        result = EMAIL.matcher(result).replaceAll("[REDACTED_EMAIL]");
        return result;
    }
}
