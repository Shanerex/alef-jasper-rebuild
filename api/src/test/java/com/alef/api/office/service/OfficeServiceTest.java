package com.alef.api.office.service;

import com.alef.api.office.dto.OfficesDto;
import com.alef.api.office.entity.OfficeEntity;
import com.alef.api.office.repository.OfficeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OfficeService mapping and ordering logic (architecture §3.2, feature 011).
 *
 * Repository is mocked; no DB or Spring context required. Verifies correct mapping
 * of entity array fields (addressLines, phones) to DTOs, ordering preservation,
 * and the empty-envelope graceful-degradation contract.
 */
@ExtendWith(MockitoExtension.class)
class OfficeServiceTest {

    @Mock
    private OfficeRepository repository;

    private OfficeService service;

    @BeforeEach
    void setUp() {
        service = new OfficeService(repository);
    }

    @Test
    void getOffices_maps_entity_fields_to_dto_including_arrays() {
        var dubai = officeEntity("dubai", "ALEF — Dubai (Head Office)",
                List.of("404 Sheikh Maktoum Building", "Damascus Street, Al Qusais", "PO Box 65825, Dubai, UAE"),
                List.of("+971 4 2513840", "+971 4 3434440"),
                "alefllc@eim.ae",
                "Sheikh Maktoum Building, Damascus Street, Al Qusais, Dubai",
                0);
        when(repository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(dubai));

        var result = service.getOffices();

        assertThat(result.offices()).hasSize(1);
        var dto = result.offices().get(0);
        assertThat(dto.key()).isEqualTo("dubai");
        assertThat(dto.name()).isEqualTo("ALEF — Dubai (Head Office)");
        assertThat(dto.addressLines()).containsExactly(
                "404 Sheikh Maktoum Building", "Damascus Street, Al Qusais", "PO Box 65825, Dubai, UAE");
        assertThat(dto.phones()).containsExactly("+971 4 2513840", "+971 4 3434440");
        assertThat(dto.email()).isEqualTo("alefllc@eim.ae");
        assertThat(dto.mapQuery()).isEqualTo("Sheikh Maktoum Building, Damascus Street, Al Qusais, Dubai");
    }

    @Test
    void getOffices_preserves_repository_ordering_dubai_before_india() {
        var dubai = officeEntity("dubai", "ALEF — Dubai (Head Office)",
                List.of("404 Sheikh Maktoum Building"), List.of("+971 4 2513840"),
                "alefllc@eim.ae", null, 0);
        var india = officeEntity("india", "Jasper — India (Delivery Centre)",
                List.of("Plot No. 2/286"), List.of("0091 4636 293166"),
                "jasperiec@gmail.com", null, 1);
        when(repository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(dubai, india));

        var result = service.getOffices();

        assertThat(result.offices()).hasSize(2);
        assertThat(result.offices().get(0).key()).isEqualTo("dubai");
        assertThat(result.offices().get(1).key()).isEqualTo("india");
    }

    @Test
    void getOffices_returns_empty_envelope_not_null_when_table_is_empty() {
        when(repository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());

        OfficesDto result = service.getOffices();

        assertThat(result).isNotNull();
        assertThat(result.offices()).isEmpty();
    }

    @Test
    void getOffices_propagates_nullable_mapQuery_as_null() {
        var india = officeEntity("india", "Jasper — India (Delivery Centre)",
                List.of("Plot No. 2/286"), List.of("0091 4636 293166"),
                "jasperiec@gmail.com", null, 1);
        when(repository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(india));

        var dto = service.getOffices().offices().get(0);

        assertThat(dto.mapQuery()).isNull();
    }

    // --- helpers ---

    private OfficeEntity officeEntity(String key, String name, List<String> addressLines,
                                       List<String> phones, String email,
                                       String mapQuery, int order) {
        var e = new OfficeEntity();
        e.setOfficeKey(key);
        e.setName(name);
        e.setAddressLines(addressLines);
        e.setPhones(phones);
        e.setEmail(email);
        e.setMapQuery(mapQuery);
        e.setDisplayOrder(order);
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        return e;
    }
}
