package com.alef.api.admin.upload;

import com.alef.api.admin.upload.dto.UploadResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * POST /api/admin/uploads -- multipart upload endpoint (design.md §A.6, F12-AC20..AC22).
 *
 * Source-agnostic: the same endpoint is fed by a desktop file dialog or a
 * phone's OS file picker / camera roll (F12-AC27) -- there is nothing
 * desktop-only about a multipart POST.
 */
@RestController
@RequestMapping("/api/admin/uploads")
public class UploadController {

    private final UploadService service;

    public UploadController(UploadService service) {
        this.service = service;
    }

    /**
     * Accepts one multipart file plus its category, validates it, stores it,
     * and returns the public-relative URL for the caller to submit on the
     * next record save (architecture §2.4: upload and record-save are
     * separate steps).
     */
    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponseDto upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam("category") String category) {
        return service.store(file, category);
    }
}
