package com.softwaremind.invoicedocbackend.contractor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractorRepository extends JpaRepository<ContractorEntity, Long> {

    List<ContractorEntity> findByOrganizationId(Long organizationId);

    Optional<ContractorEntity> findByOrganizationIdAndId(Long organizationId, Long id);

    List<ContractorEntity> findByOrganizationIdAndNameContainingIgnoreCase(Long organizationId, String namePart);
}