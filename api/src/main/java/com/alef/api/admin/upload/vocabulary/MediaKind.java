package com.alef.api.admin.upload.vocabulary;

import java.util.Set;

/**
 * The two upload media kinds and their validation rules (architecture §2.4).
 *
 * Each UploadCategory maps to exactly one kind. Keeping the type/size/magic-byte
 * rules here (rather than duplicated per category) means "images" and
 * "documents" only need to be defined once even though there are three image
 * categories and one document category (design §A.6).
 */
public enum MediaKind {

    /** project image, team photo, sample preview: jpeg/png/webp, max 5 MB. */
    IMAGE(
            Set.of("image/jpeg", "image/png", "image/webp"),
            Set.of("jpg", "jpeg", "png", "webp"),
            5L * 1024 * 1024
    ) {
        @Override
        public boolean magicBytesMatch(byte[] head) {
            return isJpeg(head) || isPng(head) || isWebp(head);
        }
    },

    /** sample downloadable file: PDF only, max 25 MB. */
    DOCUMENT(
            Set.of("application/pdf"),
            Set.of("pdf"),
            25L * 1024 * 1024
    ) {
        @Override
        public boolean magicBytesMatch(byte[] head) {
            return isPdf(head);
        }
    };

    private final Set<String> allowedContentTypes;
    private final Set<String> allowedExtensions;
    private final long maxSizeBytes;

    MediaKind(Set<String> allowedContentTypes, Set<String> allowedExtensions, long maxSizeBytes) {
        this.allowedContentTypes = allowedContentTypes;
        this.allowedExtensions = allowedExtensions;
        this.maxSizeBytes = maxSizeBytes;
    }

    public Set<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    /**
     * Sniffs the leading bytes of the uploaded file against this kind's magic
     * number(s). The declared Content-Type header is client-controlled and
     * spoofable, so this is the authoritative type check (architecture §2.4).
     */
    public abstract boolean magicBytesMatch(byte[] head);

    static boolean isJpeg(byte[] head) {
        return head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
    }

    static boolean isPng(byte[] head) {
        return head.length >= 8
                && (head[0] & 0xFF) == 0x89 && head[1] == 0x50 && head[2] == 0x4E && head[3] == 0x47
                && head[4] == 0x0D && head[5] == 0x0A && head[6] == 0x1A && head[7] == 0x0A;
    }

    static boolean isWebp(byte[] head) {
        return head.length >= 12
                && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
    }

    static boolean isPdf(byte[] head) {
        return head.length >= 4 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F';
    }
}
