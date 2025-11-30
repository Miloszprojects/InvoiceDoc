package com.softwaremind.invoicedocbackend.tenant.mapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import com.softwaremind.invoicedocbackend.common.mapper.AddressMapper;
import com.softwaremind.invoicedocbackend.crypto.CryptoService;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileEntity;
import com.softwaremind.invoicedocbackend.tenant.dto.SellerProfileCreateRequest;
import com.softwaremind.invoicedocbackend.tenant.dto.SellerProfileResponse;
import com.softwaremind.invoicedocbackend.tenant.dto.SellerProfileUpdateRequest;

@Component
@RequiredArgsConstructor
public class SellerProfileMapper {

    private final AddressMapper addressMapper;
    private final CryptoService cryptoService;

    public SellerProfileEntity fromCreateRequest(SellerProfileCreateRequest req, OrganizationEntity org) {
        return SellerProfileEntity.builder()
                .organization(org)
                .name(req.name())
                .nipEncrypted(cryptoService.encrypt(req.nip()))
                .regon(req.regon())
                .krs(req.krs())
                .bankName(req.bankName())
                .bankAccount(req.bankAccount())
                .address(addressMapper.toEmbeddable(req.address()))
                .defaultCurrency(req.defaultCurrency())
                .defaultPaymentTermDays(req.defaultPaymentTermDays())
                .build();
    }

    public SellerProfileResponse toResponse(SellerProfileEntity entity) {
        return new SellerProfileResponse(
                entity.getId(),
                entity.getName(),
                cryptoService.decrypt(entity.getNipEncrypted()),
                entity.getRegon(),
                entity.getKrs(),
                entity.getBankName(),
                entity.getBankAccount(),
                addressMapper.toDto(entity.getAddress()),
                entity.getDefaultCurrency(),
                entity.getDefaultPaymentTermDays(),
                entity.getLogoPath()
        );
    }

    public void updateEntity(SellerProfileEntity entity, SellerProfileUpdateRequest req) {
        entity.setName(req.name());

        if (req.nip() != null && !req.nip().isBlank()) {
            entity.setNipEncrypted(cryptoService.encrypt(req.nip()));
        }

        entity.setRegon(req.regon());
        entity.setKrs(req.krs());
        entity.setBankName(req.bankName());
        entity.setBankAccount(req.bankAccount());
        entity.setAddress(addressMapper.toEmbeddable(req.address()));
        entity.setDefaultCurrency(req.defaultCurrency());
        entity.setDefaultPaymentTermDays(req.defaultPaymentTermDays());
    }
}
