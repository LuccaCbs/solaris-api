package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.CashRegisterAuthorizationRequest;
import com.luccavergara.solaris.dto.CashRegisterSessionResponse;
import com.luccavergara.solaris.entity.CashRegisterReopenLog;
import com.luccavergara.solaris.entity.CashRegisterSession;
import com.luccavergara.solaris.entity.CashRegisterStatus;
import com.luccavergara.solaris.entity.PaymentMethod;
import com.luccavergara.solaris.entity.Sale;
import com.luccavergara.solaris.entity.SystemSettings;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.CashRegisterReopenLogRepository;
import com.luccavergara.solaris.repository.CashRegisterSessionRepository;
import com.luccavergara.solaris.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.luccavergara.solaris.entity.AuditAction;
import com.luccavergara.solaris.entity.AuditEntityType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashRegisterService {

    private static final String SYSTEM_AUTO_CLOSE = "SYSTEM_AUTO_CLOSE";

    private final CashRegisterSessionRepository cashRegisterSessionRepository;
    private final SaleRepository saleRepository;
    private final CashRegisterReopenLogRepository cashRegisterReopenLogRepository;
    private final SystemSettingsService systemSettingsService;
    private final AuthenticatedUserService authenticatedUserService;
    private final AuditLogService auditLogService;

    @Transactional
    public CashRegisterSessionResponse openCashRegister(
            CashRegisterAuthorizationRequest request
    ) {
        User currentUser = authenticatedUserService.getCurrentUser();

        autoCloseStaleOpenCashRegisterIfNeeded(currentUser);

        systemSettingsService.validateAdminPasswordOrThrow(request.getAdminPassword());

        cashRegisterSessionRepository
                .findFirstByStatusAndUserOrderByOpenedAtDesc(CashRegisterStatus.OPEN, currentUser)
                .ifPresent(session -> {
                    throw new IllegalStateException("There is already an open cash register session");
                });

        LocalDate today = getTodayByBusinessTimezone();

        cashRegisterSessionRepository
                .findFirstByUserAndOpenedAtBetweenOrderByOpenedAtDesc(
                        currentUser,
                        today.atStartOfDay(),
                        today.atTime(LocalTime.MAX)
                )
                .ifPresent(session -> {
                    throw new IllegalStateException("There is already a cash register session for today. Reopen it instead.");
                });

        CashRegisterSession session = CashRegisterSession.builder()
                .openedAt(LocalDateTime.now())
                .closedAt(null)
                .openedBy(currentUser.getEmail())
                .closedBy(null)
                .status(CashRegisterStatus.OPEN)
                .reopenCount(0)
                .closingAmount(BigDecimal.ZERO)
                .cashCount(0)
                .cashAmount(BigDecimal.ZERO)
                .creditCardCount(0)
                .creditCardAmount(BigDecimal.ZERO)
                .debitCardCount(0)
                .debitCardAmount(BigDecimal.ZERO)
                .transferCount(0)
                .transferAmount(BigDecimal.ZERO)
                .otherCount(0)
                .otherAmount(BigDecimal.ZERO)
                .user(currentUser)
                .build();

        CashRegisterSession savedSession = cashRegisterSessionRepository.save(session);

        auditLogService.log(
                AuditAction.OPEN_CASH_REGISTER,
                AuditEntityType.CASH_REGISTER,
                savedSession.getId(),
                "Cash Register #" + savedSession.getId(),
                "Cash register opened"
        );

        return mapToResponse(savedSession);
    }

    public CashRegisterSessionResponse getCurrentSession() {
        User currentUser = authenticatedUserService.getCurrentUser();

        autoCloseStaleOpenCashRegisterIfNeeded(currentUser);

        CashRegisterSession session = cashRegisterSessionRepository
                .findFirstByStatusAndUserOrderByOpenedAtDesc(CashRegisterStatus.OPEN, currentUser)
                .orElseThrow(() -> new IllegalStateException("There is no open cash register session"));

        return mapToResponse(session);
    }

    public CashRegisterSessionResponse getTodaySession() {
        User currentUser = authenticatedUserService.getCurrentUser();

        autoCloseStaleOpenCashRegisterIfNeeded(currentUser);

        LocalDate today = getTodayByBusinessTimezone();

        CashRegisterSession session = cashRegisterSessionRepository
                .findFirstByUserAndOpenedAtBetweenOrderByOpenedAtDesc(
                        currentUser,
                        today.atStartOfDay(),
                        today.atTime(LocalTime.MAX)
                )
                .orElseThrow(() -> new IllegalStateException("There is no cash register session for today"));

        return mapToResponse(session);
    }

    @Transactional
    public CashRegisterSessionResponse closeCashRegister(
            CashRegisterAuthorizationRequest request
    ) {
        User currentUser = authenticatedUserService.getCurrentUser();

        autoCloseStaleOpenCashRegisterIfNeeded(currentUser);

        systemSettingsService.validateAdminPasswordOrThrow(request.getAdminPassword());

        CashRegisterSession session = cashRegisterSessionRepository
                .findFirstByStatusAndUserOrderByOpenedAtDesc(CashRegisterStatus.OPEN, currentUser)
                .orElseThrow(() -> new IllegalStateException("There is no open cash register session"));

        closeSession(session, currentUser.getEmail());

        CashRegisterSession savedSession = cashRegisterSessionRepository.save(session);

        auditLogService.log(
                AuditAction.CLOSE_CASH_REGISTER,
                AuditEntityType.CASH_REGISTER,
                savedSession.getId(),
                "Cash Register #" + savedSession.getId(),
                "Cash register closed with total $" + savedSession.getClosingAmount()
        );

        return mapToResponse(savedSession);
    }

    @Transactional
    public CashRegisterSessionResponse reopenCashRegister(
            Long id,
            CashRegisterAuthorizationRequest request
    ) {
        User currentUser = authenticatedUserService.getCurrentUser();

        autoCloseStaleOpenCashRegisterIfNeeded(currentUser);

        systemSettingsService.validateAdminPasswordOrThrow(request.getAdminPassword());

        cashRegisterSessionRepository
                .findFirstByStatusAndUserOrderByOpenedAtDesc(CashRegisterStatus.OPEN, currentUser)
                .ifPresent(session -> {
                    throw new IllegalStateException("There is already an open cash register session");
                });

        CashRegisterSession session = cashRegisterSessionRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Cash register session not found"));

        if (session.getStatus() != CashRegisterStatus.CLOSED) {
            throw new IllegalStateException("Only closed cash register sessions can be reopened");
        }

        session.setClosedAt(null);
        session.setClosedBy(null);
        session.setStatus(CashRegisterStatus.OPEN);
        session.setReopenCount(session.getReopenCount() + 1);

        CashRegisterReopenLog log = CashRegisterReopenLog.builder()
                .cashRegisterSession(session)
                .reopenedBy(currentUser.getEmail())
                .reopenedAt(LocalDateTime.now())
                .build();

        cashRegisterReopenLogRepository.save(log);

        CashRegisterSession savedSession = cashRegisterSessionRepository.save(session);

        auditLogService.log(
                AuditAction.REOPEN_CASH_REGISTER,
                AuditEntityType.CASH_REGISTER,
                savedSession.getId(),
                "Cash Register #" + savedSession.getId(),
                "Cash register reopened"
        );

        return mapToResponse(savedSession);
    }
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void autoCloseOpenCashRegistersAtConfiguredTime() {
        List<SystemSettings> allSettings = systemSettingsService.getAllSettings();

        for (SystemSettings settings : allSettings) {
            ZoneId zoneId = ZoneId.of(settings.getBusinessTimezone());
            LocalTime now = LocalTime.now(zoneId).withSecond(0).withNano(0);

            if (!now.equals(settings.getCashRegisterAutoCloseTime())) {
                continue;
            }

            cashRegisterSessionRepository
                    .findFirstByStatusAndUserOrderByOpenedAtDesc(
                            CashRegisterStatus.OPEN,
                            settings.getUser()
                    )
                    .ifPresent(session -> {
                        closeSession(session, SYSTEM_AUTO_CLOSE);
                        cashRegisterSessionRepository.save(session);
                    });
        }
    }

    @Transactional
    protected void autoCloseStaleOpenCashRegisterIfNeeded(User currentUser) {
        LocalDate today = getTodayByBusinessTimezone();

        cashRegisterSessionRepository
                .findFirstByStatusAndUserOrderByOpenedAtDesc(CashRegisterStatus.OPEN, currentUser)
                .ifPresent(session -> {
                    if (!session.getOpenedAt().toLocalDate().isBefore(today)) {
                        return;
                    }

                    closeSession(session, SYSTEM_AUTO_CLOSE);
                    cashRegisterSessionRepository.save(session);
                });
    }

    private void closeSession(
            CashRegisterSession session,
            String closedBy
    ) {
        List<Sale> sessionSales = saleRepository.findAllByCashRegisterSessionIdAndUser(
                session.getId(),
                session.getUser()
        );

        applySalesTotals(session, sessionSales);

        session.setClosedAt(LocalDateTime.now());
        session.setClosedBy(closedBy);
        session.setStatus(CashRegisterStatus.CLOSED);
    }

    private void applySalesTotals(
            CashRegisterSession session,
            List<Sale> sales
    ) {
        BigDecimal totalAmount = sales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        session.setClosingAmount(totalAmount);

        session.setCashCount(countByPaymentMethod(sales, PaymentMethod.CASH));
        session.setCashAmount(sumByPaymentMethod(sales, PaymentMethod.CASH));

        session.setCreditCardCount(countByPaymentMethod(sales, PaymentMethod.CREDIT_CARD));
        session.setCreditCardAmount(sumByPaymentMethod(sales, PaymentMethod.CREDIT_CARD));

        session.setDebitCardCount(countByPaymentMethod(sales, PaymentMethod.DEBIT_CARD));
        session.setDebitCardAmount(sumByPaymentMethod(sales, PaymentMethod.DEBIT_CARD));

        session.setTransferCount(countByPaymentMethod(sales, PaymentMethod.TRANSFER));
        session.setTransferAmount(sumByPaymentMethod(sales, PaymentMethod.TRANSFER));

        session.setOtherCount(countByPaymentMethod(sales, PaymentMethod.OTHER));
        session.setOtherAmount(sumByPaymentMethod(sales, PaymentMethod.OTHER));
    }

    private Integer countByPaymentMethod(
            List<Sale> sales,
            PaymentMethod paymentMethod
    ) {
        return (int) sales.stream()
                .filter(sale -> sale.getPaymentMethod() == paymentMethod)
                .count();
    }

    private BigDecimal sumByPaymentMethod(
            List<Sale> sales,
            PaymentMethod paymentMethod
    ) {
        return sales.stream()
                .filter(sale -> sale.getPaymentMethod() == paymentMethod)
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDate getTodayByBusinessTimezone() {
        SystemSettings settings = systemSettingsService.getOrCreateSettings();
        ZoneId zoneId = ZoneId.of(settings.getBusinessTimezone());

        return LocalDate.now(zoneId);
    }

    private CashRegisterSessionResponse mapToResponse(CashRegisterSession session) {
        return CashRegisterSessionResponse.builder()
                .id(session.getId())
                .openedAt(session.getOpenedAt())
                .closedAt(session.getClosedAt())
                .openedBy(session.getOpenedBy())
                .closedBy(session.getClosedBy())
                .status(session.getStatus())
                .reopenCount(session.getReopenCount())
                .closingAmount(session.getClosingAmount())
                .cashCount(session.getCashCount())
                .cashAmount(session.getCashAmount())
                .creditCardCount(session.getCreditCardCount())
                .creditCardAmount(session.getCreditCardAmount())
                .debitCardCount(session.getDebitCardCount())
                .debitCardAmount(session.getDebitCardAmount())
                .transferCount(session.getTransferCount())
                .transferAmount(session.getTransferAmount())
                .otherCount(session.getOtherCount())
                .otherAmount(session.getOtherAmount())
                .build();
    }
}