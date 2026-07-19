package com.alef.api.admin.upload.dto;

/**
 * Response body for POST /api/admin/uploads (design.md §A.6).
 * `url` is the public-relative path the caller submits as a record's
 * image/photo/preview/file field on the next save (architecture §2.4/§3.5).
 */
public record UploadResponseDto(String url, String filename, String contentType, long size) {
}
