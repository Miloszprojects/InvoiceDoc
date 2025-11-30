package com.softwaremind.invoicedocbackend.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class OrganizationEntityTest {

    private static final Long ID = 1L;
    private static final String NAME = "Acme Organization";
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 1, 10, 12, 30);

    @Test
    @DisplayName("builder should set all fields correctly")
    void builderShouldSetAllFields() {
        OrganizationEntity entity = OrganizationEntity.builder()
                .id(ID)
                .name(NAME)
                .createdAt(CREATED_AT)
                .build();

        assertAll(
                () -> assertThat(entity.getId()).isEqualTo(ID),
                () -> assertThat(entity.getName()).isEqualTo(NAME),
                () -> assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT)
        );
    }

    @Test
    @DisplayName("setters and getters should work correctly")
    void settersAndGettersShouldWorkCorrectly() {
        OrganizationEntity entity = new OrganizationEntity();

        entity.setId(ID);
        entity.setName(NAME);
        entity.setCreatedAt(CREATED_AT);

        assertAll(
                () -> assertThat(entity.getId()).isEqualTo(ID),
                () -> assertThat(entity.getName()).isEqualTo(NAME),
                () -> assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT)
        );
    }

    @Test
    @DisplayName("no-args constructor should create an instance with null fields")
    void noArgsConstructorShouldCreateInstanceWithNullFields() {
        OrganizationEntity entity = new OrganizationEntity();

        assertAll(
                () -> assertThat(entity.getId()).isNull(),
                () -> assertThat(entity.getName()).isNull(),
                () -> assertThat(entity.getCreatedAt()).isNull()
        );
    }
}
