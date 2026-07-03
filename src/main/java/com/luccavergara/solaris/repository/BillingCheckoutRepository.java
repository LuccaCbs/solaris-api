package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.BillingCheckout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingCheckoutRepository extends JpaRepository<BillingCheckout, Long> {

    Optional<BillingCheckout> findByExternalPreferenceId(String externalPreferenceId);

    Optional<BillingCheckout> findByExternalPaymentId(String externalPaymentId);

    Optional<BillingCheckout> findByIdAndOrganizationId(Long id, Long organizationId);
}
