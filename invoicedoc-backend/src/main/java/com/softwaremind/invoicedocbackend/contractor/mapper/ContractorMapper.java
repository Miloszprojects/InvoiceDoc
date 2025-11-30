package com.softwaremind.invoicedocbackend.contractor.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.softwaremind.invoicedocbackend.common.mapper.AddressMapper;
import com.softwaremind.invoicedocbackend.contractor.ContractorEntity;
import com.softwaremind.invoicedocbackend.contractor.dto.ContractorCreateRequest;
import com.softwaremind.invoicedocbackend.contractor.dto.ContractorResponse;
import com.softwaremind.invoicedocbackend.contractor.dto.ContractorUpdateRequest;
import com.softwaremind.invoicedocbackend.crypto.CryptoService;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;

@Component
@RequiredArgsConstructor
public class ContractorMapper {

    private final AddressMapper addressMapper;
    private final CryptoService cryptoService;

    public ContractorEntity fromCreateRequest(ContractorCreateRequest req, OrganizationEntity org) {
        return ContractorEntity.builder()
                .organization(org)
                .type(req.type())
                .name(req.name())
                .nipEncrypted(req.nip() != null ? cryptoService.encrypt(req.nip()) : null)
                .peselEncrypted(req.pesel() != null ? cryptoService.encrypt(req.pesel()) : null)
                .address(addressMapper.toEmbeddable(req.address()))
                .email(req.email())
                .phone(req.phone())
                .favorite(Boolean.TRUE.equals(req.favorite()))
                .build();
    }

    public void updateEntity(ContractorEntity entity, ContractorUpdateRequest req) {
        entity.setType(req.type());
        entity.setName(req.name());
        if (req.nip() != null) {
            entity.setNipEncrypted(cryptoService.encrypt(req.nip()));
        } else {
            entity.setNipEncrypted(null);
        }
        if (req.pesel() != null) {
            entity.setPeselEncrypted(cryptoService.encrypt(req.pesel()));
        } else {
            entity.setPeselEncrypted(null);
        }
        entity.setAddress(addressMapper.toEmbeddable(req.address()));
        entity.setEmail(req.email());
        entity.setPhone(req.phone());
        entity.setFavorite(Boolean.TRUE.equals(req.favorite()));
    }

    public ContractorResponse toResponse(ContractorEntity entity) {
        return new ContractorResponse(
                entity.getId(),
                entity.getType(),
                entity.getName(),
                entity.getNipEncrypted() != null ? cryptoService.decrypt(entity.getNipEncrypted()) : null,
                entity.getPeselEncrypted() != null ? cryptoService.decrypt(entity.getPeselEncrypted()) : null,
                addressMapper.toDto(entity.getAddress()),
                entity.getEmail(),
                entity.getPhone(),
                entity.getFavorite()
        );
    }
}
