package com.alef.api.team.service;

import com.alef.api.team.dto.TeamOverviewDto;
import com.alef.api.team.entity.TeamEntity;
import com.alef.api.team.repository.TeamRepository;
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
 * Unit tests for TeamService mapping and ordering logic (architecture §3.1, feature 011).
 *
 * Repository is mocked; no DB or Spring context required. Verifies that the
 * service maps entity fields to DTOs correctly, preserves display_order, and
 * returns an empty envelope (not null) when no active profiles exist.
 */
@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository repository;

    private TeamService service;

    @BeforeEach
    void setUp() {
        service = new TeamService(repository);
    }

    @Test
    void getTeam_maps_entity_fields_to_dto() {
        var entity = member("K. Jeyaraman", "Managing Director", "ALEF & JASPER",
                "jeyaraman@alef-jasper.com", null, 1);
        when(repository.findAllByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(entity));

        var result = service.getTeam();

        assertThat(result.members()).hasSize(1);
        var dto = result.members().get(0);
        assertThat(dto.name()).isEqualTo("K. Jeyaraman");
        assertThat(dto.role()).isEqualTo("Managing Director");
        assertThat(dto.company()).isEqualTo("ALEF & JASPER");
        assertThat(dto.email()).isEqualTo("jeyaraman@alef-jasper.com");
        assertThat(dto.photo()).isNull();
    }

    @Test
    void getTeam_preserves_repository_ordering() {
        var first = member("Ali Bin Beyat", "Sponsor / Chairman", "ALEF", null, null, 1);
        var second = member("K. Jeyaraman", "Managing Director", "ALEF & JASPER",
                "jeyaraman@alef-jasper.com", null, 2);
        when(repository.findAllByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(first, second));

        var result = service.getTeam();

        assertThat(result.members()).hasSize(2);
        assertThat(result.members().get(0).name()).isEqualTo("Ali Bin Beyat");
        assertThat(result.members().get(1).name()).isEqualTo("K. Jeyaraman");
    }

    @Test
    void getTeam_returns_empty_envelope_not_null_when_no_active_members() {
        when(repository.findAllByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of());

        TeamOverviewDto result = service.getTeam();

        assertThat(result).isNotNull();
        assertThat(result.members()).isEmpty();
    }

    @Test
    void getTeam_propagates_nullable_fields_as_null() {
        var entity = member("Namasivayam", "Administration Manager", null, null, null, 6);
        when(repository.findAllByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(entity));

        var dto = service.getTeam().members().get(0);

        assertThat(dto.company()).isNull();
        assertThat(dto.email()).isNull();
        assertThat(dto.photo()).isNull();
    }

    // --- helpers ---

    private TeamEntity member(String name, String role, String company,
                               String email, String photo, int order) {
        var e = new TeamEntity();
        e.setName(name);
        e.setRole(role);
        e.setCompany(company);
        e.setEmail(email);
        e.setPhoto(photo);
        e.setDisplayOrder(order);
        e.setActive(true);
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        return e;
    }
}
