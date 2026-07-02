package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.SystemSettings;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {

    Optional<SystemSettings> findByUser(User user);

    Optional<SystemSettings> findByOrganizationId(Long organizationId);

    List<SystemSettings> findAll();
}
