package com.alef.api.admin.team;

import com.alef.api.admin.error.AdminResourceNotFoundException;
import com.alef.api.admin.team.dto.AdminTeamRequest;
import com.alef.api.team.entity.TeamEntity;
import com.alef.api.team.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AdminTeamService (design.md §A.3, F12-AC8..AC11).
 */
@ExtendWith(MockitoExtension.class)
class AdminTeamServiceTest {

    @Mock
    private TeamRepository repository;

    private AdminTeamService service;

    @BeforeEach
    void setUp() {
        service = new AdminTeamService(repository);
    }

    @Test
    void listAll_returns_all_rows_including_inactive() {
        TeamEntity active = entity("Active Person", true);
        TeamEntity inactive = entity("Inactive Person", false);
        when(repository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(active, inactive));

        var result = service.listAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting("active").containsExactly(true, false);
    }

    @Test
    void create_requires_only_name_and_role_at_the_service_level() {
        var request = new AdminTeamRequest("New Hire", "Detailer", null, null, null, 0, true);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.create(request);

        assertThat(dto.name()).isEqualTo("New Hire");
        assertThat(dto.role()).isEqualTo("Detailer");
    }

    @Test
    void update_can_toggle_active_to_false_for_soft_hide() {
        TeamEntity existing = entity("K. Jeyaraman", true);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new AdminTeamRequest("K. Jeyaraman", "Managing Director", "ALEF", null, null, 0, false);
        var dto = service.update(1L, request);

        assertThat(dto.active()).isFalse();
    }

    @Test
    void update_unknown_id_throws_not_found() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L,
                new AdminTeamRequest("X", "Y", null, null, null, 0, true)))
                .isInstanceOf(AdminResourceNotFoundException.class);
    }

    @Test
    void delete_hard_deletes_the_row() {
        TeamEntity existing = entity("To Remove", true);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));

        service.delete(5L);

        verify(repository).delete(existing);
    }

    private TeamEntity entity(String name, boolean active) {
        TeamEntity e = new TeamEntity();
        e.setName(name);
        e.setRole("Role");
        e.setActive(active);
        e.setDisplayOrder(0);
        e.setCreatedAt(java.time.Instant.now());
        e.setUpdatedAt(java.time.Instant.now());
        return e;
    }
}
