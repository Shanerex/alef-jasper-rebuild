package com.alef.api.office.service;

import com.alef.api.office.dto.OfficeDto;
import com.alef.api.office.dto.OfficesDto;
import com.alef.api.office.repository.OfficeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reads office location data for the Contact page (architecture §3.2, feature 011).
 *
 * Thin mapping layer: ordering and selection are delegated to the repository query.
 * Feature 012 will add write methods; the read path stays unchanged.
 */
@Service
@Transactional(readOnly = true)
public class OfficeService {

    private final OfficeRepository repository;

    public OfficeService(OfficeRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns all offices in display_order sequence, mapped to DTOs.
     *
     * Returns an empty envelope rather than null when the table is empty,
     * so the API always returns a valid JSON object (graceful degradation).
     * The Contact page renders a loading fallback when the list is empty.
     */
    public OfficesDto getOffices() {
        List<OfficeDto> offices = repository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(e -> new OfficeDto(
                        e.getOfficeKey(),
                        e.getName(),
                        e.getAddressLines(),
                        e.getPhones(),
                        e.getEmail(),
                        e.getMapQuery()))
                .toList();
        return new OfficesDto(offices);
    }
}
