package com.softwaremind.invoicedocbackend.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItemEntity, Long> {
}
