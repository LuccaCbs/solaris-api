package com.luccavergara.solaris.service;

import com.luccavergara.solaris.entity.*;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.OrganizationMemberRepository;
import com.luccavergara.solaris.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrganizationMembershipService {

    private final OrganizationMemberRepository organizationMemberRepository;
    private final StoreRepository storeRepository;

    public Optional<OrganizationMember> findPrimaryMembership(User user) {
        List<OrganizationMember> activeMembers = organizationMemberRepository.findAllByUserAndStatus(
                user,
                OrganizationMemberStatus.ACTIVE
        );

        return activeMembers.stream()
                .filter(member -> member.getRole() == OrganizationMemberRole.OWNER)
                .findFirst()
                .or(() -> activeMembers.stream().findFirst());
    }

    public OrganizationMember resolveMembershipForOrganization(User user, Long organizationId) {
        return organizationMemberRepository
                .findByUserAndOrganizationIdAndStatus(user, organizationId, OrganizationMemberStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Organization membership not found"));
    }

    public void validateStoreInOrganization(Long organizationId, Long storeId) {
        storeRepository.findById(storeId)
                .filter(store -> store.getOrganization().getId().equals(organizationId))
                .orElseThrow(() -> new ResourceNotFoundException("Store not found in organization"));
    }

    public Map<String, Object> buildJwtClaims(OrganizationMember membership) {
        return buildJwtClaims(membership, null);
    }

    public Map<String, Object> buildJwtClaims(OrganizationMember membership, Long storeIdOverride) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("orgId", membership.getOrganization().getId());
        claims.put("role", membership.getRole().name());

        Long storeId = storeIdOverride;

        if (storeId == null && membership.getStore() != null) {
            storeId = membership.getStore().getId();
        }

        if (storeId == null) {
            storeId = storeRepository.findAllByOrganizationId(membership.getOrganization().getId()).stream()
                    .filter(store -> Boolean.TRUE.equals(store.getActive()))
                    .map(Store::getId)
                    .findFirst()
                    .orElse(null);
        }

        if (storeId != null) {
            claims.put("storeId", storeId);
        }

        return claims;
    }
}
