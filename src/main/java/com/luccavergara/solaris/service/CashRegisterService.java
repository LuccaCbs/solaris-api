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
import com.luccavergara.solaris.util.BusinessTimezoneHelper;
import com.luccavergara.solaris.entity.AuditAction;
import com.luccavergara.solaris.entity.AuditEntityType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CashRegisterService {

    private static final String SYSTEM_AUTO_CLOSE = "SYSTEM_AUTO_CLOSE";

    private final CashRegisterSessionRepository cashRegisterSessionRepository;
    private final CashRegisterReopenLogRepository cashRegisterReopenLogRepository;
    private final SaleRepository saleRepository;
    private final SystemSettingsService systemSettingsService;
    private final AuthenticatedUserService authenticatedUserService;
    private final AuditLogService auditLogService;
    private final TenantQueryService tenantQueryService;
    private final TenantScopeService tenantScopeService;
    private final BusinessTimezoneHelper businessTimezoneHelper;

    @Transactional
    public CashRegisterSessionResponse openCashRegister(
            CashRegisterAuthorizationRequest request
    ) {
        User currentUser = authenticatedUserService.getCurrentUser();

        autoCloseStaleOpenCashRegisterIfNeeded(currentUser);

        systemSettingsService.validateAdminPasswordOrThrow(request.getAdminPassword());

        tenantQueryService.findOpenCashRegisterSession()
                .ifPresent(session -> {
                    throw new IllegalStateException("There is already an open cash register session");
                });

        LocalDate today = businessTimezoneHelper.today();

        findSessionForBusinessDay(today)
                .ifPresent(session -> {
                    throw new IllegalStateException("There is already a cash register session for today. Reopen it instead.");
                });

        CashRegisterSession session = CashRegisterSession.builder()
                .openedAt(businessTimezoneHelper.nowForStorage())
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

        tenantScopeService.getOrganizationReference(currentUser)
                .ifPresent(organization -> {
                    session.setOrganization(organization);
                    session.setCreatedBy(currentUser);
                });

        tenantScopeService.getStoreReference(currentUser)
                .ifPresent(session::setStore);

        if (session.getOrganization() != null && session.getStore() == null) {
            throw new IllegalStateException("No store assigned for cash register");
        }

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

        CashRegisterSession session = tenantQueryService.findOpenCashRegisterSession()
                .orElseThrow(() -> new IllegalStateException("There is no open cash register session"));

        return mapToResponse(session);
    }

    public CashRegisterSessionResponse getTodaySession() {
        User currentUser = authenticatedUserService.getCurrentUser();

        autoCloseStaleOpenCashRegisterIfNeeded(currentUser);

        LocalDate today = businessTimezoneHelper.today();

        CashRegisterSession session = findSessionForBusinessDay(today)
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

        CashRegisterSession session = tenantQueryService.findOpenCashRegisterSession()
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

        tenantQueryService.findOpenCashRegisterSession()
                .ifPresent(session -> {
                    throw new IllegalStateException("There is already an open cash register session");
                });

        CashRegisterSession session = tenantQueryService.findCashRegisterSessionById(id)
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
                .reopenedAt(businessTimezoneHelper.nowForStorage())
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

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void autoCloseOpenCashRegistersAtConfiguredTime() {
        List<SystemSettings> allSettings = systemSettingsService.getAllSettings();

        for (SystemSettings settings : allSettings) {
            ZoneId zoneId = ZoneId.of(settings.getBusinessTimezone());
            LocalTime now = LocalTime.now(zoneId).withSecond(0).withNano(0);

            if (!now.equals(settings.getCashRegisterAutoCloseTime())) {
                continue;
            }

            if (settings.getOrganization() != null) {
                List<CashRegisterSession> openSessions = cashRegisterSessionRepository
                        .findAllByStatusAndOrganizationId(
                                CashRegisterStatus.OPEN,
                                settings.getOrganization().getId()
                        );

                for (CashRegisterSession session : openSessions) {
                    closeSession(session, SYSTEM_AUTO_CLOSE);
                    cashRegisterSessionRepository.save(session);
                }

                continue;
            }

            cashRegisterSessionRepository.findFirstByStatusAndUserOrderByOpenedAtDesc(
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
        LocalDate today = businessTimezoneHelper.today();

        tenantQueryService.findOpenCashRegisterSession(currentUser)
                .ifPresent(session -> {
                    LocalDate openedBusinessDate = businessTimezoneHelper.toBusinessDate(session.getOpenedAt());

                    if (!openedBusinessDate.isBefore(today)) {
                        return;
                    }

                    closeSession(session, SYSTEM_AUTO_CLOSE);
                    cashRegisterSessionRepository.save(session);
                });
    }

    private Optional<CashRegisterSession> findSessionForBusinessDay(LocalDate businessDate) {
        LocalDateTime wideStart = businessTimezoneHelper.businessDayStartForQuery(businessDate.minusDays(1));
        LocalDateTime wideEnd = businessTimezoneHelper.businessDayEndForQuery(businessDate.plusDays(1));

        return tenantQueryService.findCashRegisterSessionsBetween(wideStart, wideEnd).stream()
                .filter(session -> businessTimezoneHelper.toBusinessDate(session.getOpenedAt()).equals(businessDate))
                .max(Comparator.comparing(CashRegisterSession::getOpenedAt));
    }

    private void closeSession(
            CashRegisterSession session,
            String closedBy
    ) {
        List<Sale> sessionSales = resolveSessionSales(session);

        applySalesTotals(session, sessionSales);

        session.setClosedAt(businessTimezoneHelper.nowForStorage());
        session.setClosedBy(closedBy);
        session.setStatus(CashRegisterStatus.CLOSED);
    }

    private List<Sale> resolveSessionSales(CashRegisterSession session) {
        if (session.getOrganization() != null) {
            return saleRepository.findAllByCashRegisterSessionIdAndOrganizationId(
                    session.getId(),
                    session.getOrganization().getId()
            );
        }

        return saleRepository.findAllByCashRegisterSessionIdAndUser(
                session.getId(),
                session.getUser()
        );
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

    private CashRegisterSessionResponse mapToResponse(CashRegisterSession session) {
        return CashRegisterSessionResponse.builder()
                .id(session.getId())
                .storeId(session.getStore() != null ? session.getStore().getId() : null)
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
