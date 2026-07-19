package com.alef.api.admin.upload.vocabulary;

import java.util.Arrays;
import java.util.Optional;

/**
 * The four upload categories accepted by POST /api/admin/uploads (design §A.6).
 *
 * Each category has its own storage subfolder (architecture §2.4: "foldered by
 * category") and a MediaKind that determines its allowed content types,
 * extensions, and size cap.
 */
public enum UploadCategory {

    PROJECT_IMAGE("project-image", "projects", MediaKind.IMAGE),
    TEAM_PHOTO("team-photo", "team", MediaKind.IMAGE),
    SAMPLE_PREVIEW("sample-preview", "samples/previews", MediaKind.IMAGE),
    SAMPLE_FILE("sample-file", "samples/files", MediaKind.DOCUMENT);

    private final String wireValue;
    private final String subfolder;
    private final MediaKind mediaKind;

    UploadCategory(String wireValue, String subfolder, MediaKind mediaKind) {
        this.wireValue = wireValue;
        this.subfolder = subfolder;
        this.mediaKind = mediaKind;
    }

    public String wireValue() {
        return wireValue;
    }

    public String subfolder() {
        return subfolder;
    }

    public MediaKind mediaKind() {
        return mediaKind;
    }

    /** Resolves a wire token to its category, or empty if unrecognized (design §A.6: 400 if unknown). */
    public static Optional<UploadCategory> fromWireValue(String value) {
        return Arrays.stream(values()).filter(c -> c.wireValue.equals(value)).findFirst();
    }
}
