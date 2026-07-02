package com.luccavergara.solaris.service;

import com.luccavergara.solaris.entity.Organization;
import com.luccavergara.solaris.entity.OrganizationMember;
import com.luccavergara.solaris.entity.OrganizationMemberRole;
import com.luccavergara.solaris.entity.OrganizationMemberStatus;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.repository.OrganizationMemberRepository;
import com.luccavergara.solaris.repository.OrganizationRepository;
import com.luccavergara.solaris.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class TenantScopeService {

    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationRepository organizationRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public User getCurrentUser() {
        return authenticatedUserService.getCurrentUser();
    }

    public Optional<Long> resolveOrganizationId(User user) {
        Long fromContext = TenantContext.getOrganizationId();
        if (fromContext != null) {
            return Optional.of(fromContext);
        }

        return resolvePrimaryMembership(user)
                .map(member -> member.getOrganization().getId());
    }

    public Optional<OrganizationMember> resolvePrimaryMembership(User user) {
        return organizationMemberRepository.findFirstByUserAndRoleAndStatus(
                        user,
                        OrganizationMemberRole.OWNER,
                        OrganizationMemberStatus.ACTIVE
                )
                .or(() -> organizationMemberRepository.findFirstByUserAndStatus(
                        user,
                        OrganizationMemberStatus.ACTIVE
                ));
    }

    public Optional<OrganizationMember> findActiveMembership(User user, Long organizationId) {
        return organizationMemberRepository.findByUserAndOrganizationIdAndStatus(
                user,
                organizationId,
                OrganizationMemberStatus.ACTIVE
        );
    }

    public Optional<Organization> resolveOrganization(User user) {
        return resolveOrganizationId(user)
                .flatMap(organizationRepository::findById);
    }

    public Optional<Organization> getOrganizationReference(User user) {
        return resolveOrganization(user);
    }

    public List<Long> resolveOrganizationMemberUserIds(Long organizationId) {
        return organizationMemberRepository.findUserIdsByOrganizationIdAndStatus(
                organizationId,
                OrganizationMemberStatus.ACTIVE
        );
    }

    public <T> T query(
            User user,
            Function<Long, T> byOrganization,
            Function<User, T> byUser
    ) {
        return resolveOrganizationId(user)
                .map(byOrganization)
                .orElseGet(() -> byUser.apply(user));
    }
}
