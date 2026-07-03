package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.PromoCode;
import com.luccavergara.solaris.entity.PromoCodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

    Optional<PromoCode> findByCodeNormalized(String codeNormalized);

    boolean existsByCodeNormalized(String codeNormalized);

    List<PromoCode> findAllByOrderByCreatedAtDesc();

    List<PromoCode> findAllByStatusOrderByCreatedAtDesc(PromoCodeStatus status);
}
