package com.luccavergara.solaris.service;

import com.luccavergara.solaris.entity.*;
import com.luccavergara.solaris.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationMigrationService {

    private static final String DEFAULT_STORE_NAME = "Sucursal principal";
    private static final String DEFAULT_TIMEZONE = "America/Argentina/Buenos_Aires";

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final EntityManager entityManager;
    private final SubscriptionService subscriptionService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateExistingUsersToOrganizations() {
        List<User> users = userRepository.findAll();
        int migratedUsers = 0;

        for (User user : users) {
            if (organizationMemberRepository.existsByUser(user)) {
                continue;
            }

            Organization organization = organizationRepository.save(
                    Organization.builder()
                            .razonSocial(buildDefaultRazonSocial(user))
                            .condicionIva(CondicionIva.MONOTRIBUTO)
                            .timezone(DEFAULT_TIMEZONE)
                            .build()
            );

            Store store = storeRepository.save(
                    Store.builder()
                            .organization(organization)
                            .name(DEFAULT_STORE_NAME)
                            .active(true)
                            .build()
            );

            subscriptionService.ensureSubscription(organization);

            organizationMemberRepository.save(
                    OrganizationMember.builder()
                            .user(user)
                            .organization(organization)
                            .role(OrganizationMemberRole.OWNER)
                            .store(store)
                            .status(OrganizationMemberStatus.ACTIVE)
                            .build()
            );

            backfillOrganizationId(user, organization);
            migratedUsers++;
        }

        if (migratedUsers > 0) {
            log.info("Organization migration completed for {} user(s)", migratedUsers);
        }
    }

    private void backfillOrganizationId(User user, Organization organization) {
        entityManager.createNativeQuery(
                        "UPDATE products SET organization_id = :orgId, created_by_user_id = COALESCE(created_by_user_id, user_id) WHERE user_id = :userId AND organization_id IS NULL")
                .setParameter("orgId", organization.getId())
                .setParameter("userId", user.getId())
                .executeUpdate();

        entityManager.createNativeQuery(
                        "UPDATE categories SET organization_id = :orgId, created_by_user_id = COALESCE(created_by_user_id, user_id) WHERE user_id = :userId AND organization_id IS NULL")
                .setParameter("orgId", organization.getId())
                .setParameter("userId", user.getId())
                .executeUpdate();

        entityManager.createNativeQuery(
                        "UPDATE suppliers SET organization_id = :orgId, created_by_user_id = COALESCE(created_by_user_id, user_id) WHERE user_id = :userId AND organization_id IS NULL")
                .setParameter("orgId", organization.getId())
                .setParameter("userId", user.getId())
                .executeUpdate();

        entityManager.createNativeQuery(
                        "UPDATE sales SET organization_id = :orgId, created_by_user_id = COALESCE(created_by_user_id, user_id) WHERE user_id = :userId AND organization_id IS NULL")
                .setParameter("orgId", organization.getId())
                .setParameter("userId", user.getId())
                .executeUpdate();

        entityManager.createNativeQuery(
                        "UPDATE stock_movements SET organization_id = :orgId, created_by_user_id = COALESCE(created_by_user_id, user_id) WHERE user_id = :userId AND organization_id IS NULL")
                .setParameter("orgId", organization.getId())
                .setParameter("userId", user.getId())
                .executeUpdate();

        entityManager.createNativeQuery(
                        "UPDATE supplier_orders SET organization_id = :orgId, created_by_user_id = COALESCE(created_by_user_id, user_id) WHERE user_id = :userId AND organization_id IS NULL")
                .setParameter("orgId", organization.getId())
                .setParameter("userId", user.getId())
                .executeUpdate();

        entityManager.createNativeQuery(
                        "UPDATE system_settings SET organization_id = :orgId WHERE user_id = :userId AND organization_id IS NULL")
                .setParameter("orgId", organization.getId())
                .setParameter("userId", user.getId())
                .executeUpdate();

        entityManager.createNativeQuery(
                        "UPDATE cash_register_sessions SET organization_id = :orgId, created_by_user_id = COALESCE(created_by_user_id, user_id) WHERE user_id = :userId AND organization_id IS NULL")
                .setParameter("orgId", organization.getId())
                .setParameter("userId", user.getId())
                .executeUpdate();
    }

    private String buildDefaultRazonSocial(User user) {
        return user.getFirstname() + " " + user.getLastname();
    }
}
