package com.rorfost.schoolportal.tenant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class TenantContextTest {

    @Test
    void shouldCreateValidTenantContext() {
        UUID schoolId = UUID.randomUUID();
        TenantContext context = new TenantContext(schoolId, "dhadhana-primary");

        assertThat(context.schoolId()).isEqualTo(schoolId);
        assertThat(context.slug()).isEqualTo("dhadhana-primary");
    }

    @Test
    void shouldRejectNullSchoolId() {
        assertThatThrownBy(() -> new TenantContext(null, "dhadhana"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("schoolId");
    }

    @Test
    void shouldRejectNullOrBlankSlug() {
        UUID schoolId = UUID.randomUUID();
        assertThatThrownBy(() -> new TenantContext(schoolId, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("slug");

        assertThatThrownBy(() -> new TenantContext(schoolId, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("slug must not be blank");
    }
}
