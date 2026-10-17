package com.rorfost.schoolportal.featureflag.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Registry of platform features.
 *
 * <p>Each key defines its default enablement state across all tenant schools. Existing School 1
 * features default to {@code true}. Unimplemented or planned features default to {@code false}.
 */
public enum FeatureKey {
    GALLERY("Photo Gallery", true),
    MATERIALS("Study Materials", true),
    NOTICES("Notice Board", true),
    DOWNLOADS("Official Downloads", true),
    EXAM_RESULTS("Examination Results & Marksheets", true),
    TIMETABLE("Timetable Management", false),
    ATTENDANCE("Student Attendance Tracking", false);

    private final String displayName;
    private final boolean defaultEnabled;

    FeatureKey(String displayName, boolean defaultEnabled) {
        this.displayName = displayName;
        this.defaultEnabled = defaultEnabled;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public static Optional<FeatureKey> fromString(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String normalized = key.strip().toUpperCase();
        return Arrays.stream(values()).filter(fk -> fk.name().equals(normalized)).findFirst();
    }
}
