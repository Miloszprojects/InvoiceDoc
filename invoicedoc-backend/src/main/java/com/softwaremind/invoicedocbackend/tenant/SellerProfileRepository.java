package com.softwaremind.invoicedocbackend.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerProfileRepository extends JpaRepository<SellerProfileEntity, Long> {

    List<SellerProfileEntity> findByOrganizationId(Long organizationId);
    Optional<SellerProfileEntity> findByOrganizationIdAndId(Long organizationId, Long id);
}
