package com.softwaremind.invoicedocbackend.invoice;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

    Page<InvoiceEntity> findByOrganizationId(Long organizationId, Pageable pageable);

    Page<InvoiceEntity> findByOrganizationIdAndIssueDateBetween(
            Long organizationId, LocalDate from, LocalDate to, Pageable pageable);


    boolean existsByOrganizationIdAndNumber(Long organizationId, String number);
    boolean existsByContractorId(Long contractorId);

    @Modifying
    @Query("update InvoiceEntity i set i.contractor = null where i.contractor.id = :contractorId")
    void clearContractorForInvoices(@Param("contractorId") Long contractorId);

        @Modifying
        @Query("update InvoiceEntity i set i.sellerProfile = null " +
                "where i.sellerProfile.id = :sellerProfileId")
        void clearSellerProfileForInvoices(@Param("sellerProfileId") Long sellerProfileId);

    long countByOrganizationIdAndIssueDateBetween(
            Long organizationId,
            LocalDate from,
            LocalDate to
    );
}
