package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.CreateStoreRequest;
import com.luccavergara.solaris.dto.StoreResponse;
import com.luccavergara.solaris.dto.UpdateStoreRequest;
import com.luccavergara.solaris.entity.Organization;
import com.luccavergara.solaris.entity.Store;
import com.luccavergara.solaris.exception.DuplicateResourceException;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.OrganizationRepository;
import com.luccavergara.solaris.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;
    private final SubscriptionService subscriptionService;

    @Transactional
    public StoreResponse createStore(Long organizationId, CreateStoreRequest request) {
        subscriptionService.assertCanCreateStore(organizationId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        String normalizedName = request.getName().trim();

        storeRepository.findByOrganizationAndName(organization, normalizedName)
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("A store with this name already exists");
                });

        Store store = storeRepository.save(
                Store.builder()
                        .organization(organization)
                        .name(normalizedName)
                        .address(trimToNull(request.getAddress()))
                        .afipPuntoVenta(request.getAfipPuntoVenta())
                        .active(true)
                        .build()
        );

        return mapToResponse(store);
    }

    @Transactional
    public StoreResponse updateStore(Long organizationId, Long storeId, UpdateStoreRequest request) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Store store = storeRepository.findByIdAndOrganizationId(storeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        String normalizedName = request.getName().trim();

        storeRepository.findByOrganizationAndName(organization, normalizedName)
                .filter(existing -> !existing.getId().equals(storeId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("A store with this name already exists");
                });

        store.setName(normalizedName);
        store.setAddress(trimToNull(request.getAddress()));
        store.setAfipPuntoVenta(request.getAfipPuntoVenta());

        return mapToResponse(storeRepository.save(store));
    }

    private StoreResponse mapToResponse(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .address(store.getAddress())
                .afipPuntoVenta(store.getAfipPuntoVenta())
                .active(store.getActive())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
