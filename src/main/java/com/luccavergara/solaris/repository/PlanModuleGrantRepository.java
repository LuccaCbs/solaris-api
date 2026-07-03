package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.ModuleCode;
import com.luccavergara.solaris.entity.PlanModuleGrant;
import com.luccavergara.solaris.entity.PlanModuleGrantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlanModuleGrantRepository extends JpaRepository<PlanModuleGrant, PlanModuleGrantId> {

    @Query("SELECT grant.moduleCode FROM PlanModuleGrant grant WHERE grant.planCode = :planCode")
    List<ModuleCode> findModuleCodesByPlanCode(@Param("planCode") String planCode);
}
