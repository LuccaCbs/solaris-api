package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.SubscriptionPlan;
import com.luccavergara.solaris.entity.SubscriptionPlanCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, SubscriptionPlanCode> {
}
