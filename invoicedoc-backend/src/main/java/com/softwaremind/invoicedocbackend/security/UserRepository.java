package com.softwaremind.invoicedocbackend.security;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findAllByOrganizationId(Long organizationId);

    boolean existsByOrganizationIdAndRole(Long organizationId, UserRole role);
}
