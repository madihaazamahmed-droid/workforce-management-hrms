package com.deepthought.workforce.service;

import com.deepthought.workforce.dto.OvertimeSummaryResponse;
import com.deepthought.workforce.dto.SettlementResponse;
import com.deepthought.workforce.entity.OvertimeEntry;
import com.deepthought.workforce.entity.Worker;
import com.deepthought.workforce.enums.SettlementStatus;
import com.deepthought.workforce.event.OvertimeSettledEvent;
import com.deepthought.workforce.exception.WorkforceException;
import com.deepthought.workforce.repository.OvertimeRepository;
import com.deepthought.workforce.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OvertimeService {

    private final WorkerRepository workerRepository;
    private final OvertimeRepository overtimeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public OvertimeSummaryResponse getSummary(Long workerId, String month) {
        Worker worker = findWorker(workerId);
        YearMonth ym = parseMonth(month);

        List<OvertimeEntry> entries = overtimeRepository.findByWorkerAndMonth(
                workerId, ym.getYear(), ym.getMonthValue());

        BigDecimal totalHours = entries.stream()
                .map(OvertimeEntry::getOvertimeHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = entries.stream()
                .map(OvertimeEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean anyPending = entries.stream()
                .anyMatch(e -> e.getSettlementStatus() == SettlementStatus.PENDING);

        List<OvertimeSummaryResponse.OvertimeDayBreakdown> breakdown = entries.stream()
                .map(e -> OvertimeSummaryResponse.OvertimeDayBreakdown.builder()
                        .date(e.getEntryDate())
                        .overtimeHours(e.getOvertimeHours())
                        .rateApplied(e.getOvertimeRateApplied())
                        .amount(e.getAmount())
                        .status(e.getSettlementStatus())
                        .build())
                .toList();

        return OvertimeSummaryResponse.builder()
                .workerId(workerId)
                .workerName(worker.getName())
                .month(month)
                .totalOvertimeHours(totalHours)
                .totalPayoutAmount(totalAmount)
                .overallStatus(anyPending ? SettlementStatus.PENDING : SettlementStatus.SETTLED)
                .breakdown(breakdown)
                .build();
    }

    /**
     * LF-204 FIX:
     * - Entire settlement is one @Transactional — all entries commit or none do.
     * - SMS fires via @TransactionalEventListener(AFTER_COMMIT) — only if DB succeeds.
     * - No Spring proxy trap: this method is called from the controller, not from within this bean.
     */
    @Transactional
    public SettlementResponse settle(Long workerId, String month) {
        Worker worker = findWorker(workerId);
        YearMonth ym = parseMonth(month);

        // Business rule: cannot settle current month
        YearMonth current = YearMonth.now();
        if (!ym.isBefore(current)) {
            throw WorkforceException.badRequest("CANNOT_SETTLE_CURRENT_MONTH",
                    "Only completed months can be settled. Requested: " + month);
        }

        List<OvertimeEntry> pending = overtimeRepository.findByWorkerMonthAndStatus(
                workerId, ym.getYear(), ym.getMonthValue(), SettlementStatus.PENDING);

        if (pending.isEmpty()) {
            // Check if already fully settled
            List<OvertimeEntry> all = overtimeRepository.findByWorkerAndMonth(
                    workerId, ym.getYear(), ym.getMonthValue());
            if (!all.isEmpty()) {
                throw WorkforceException.conflict("ALREADY_SETTLED",
                        "All overtime entries for " + month + " are already settled");
            }
            throw WorkforceException.notFound("NO_OVERTIME_FOUND",
                    "No overtime entries found for worker " + workerId + " in " + month);
        }

        BigDecimal totalAmount = pending.stream()
                .map(OvertimeEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Mark all SETTLED atomically — if any fail, entire transaction rolls back
        pending.forEach(e -> e.setSettlementStatus(SettlementStatus.SETTLED));
        overtimeRepository.saveAll(pending);

        // Publish event — SMS fires AFTER_COMMIT via SmsNotificationListener
        eventPublisher.publishEvent(
                new OvertimeSettledEvent(workerId, worker.getName(), month, totalAmount));

        return SettlementResponse.builder()
                .workerId(workerId)
                .workerName(worker.getName())
                .month(month)
                .entriesSettled(pending.size())
                .totalAmountSettled(totalAmount)
                .message("Settlement successful")
                .build();
    }

    private Worker findWorker(Long workerId) {
        return workerRepository.findById(workerId)
                .orElseThrow(() -> WorkforceException.notFound("WORKER_NOT_FOUND",
                        "Worker not found: " + workerId));
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (Exception e) {
            throw WorkforceException.badRequest("INVALID_MONTH_FORMAT",
                    "Month must be in YYYY-MM format, got: " + month);
        }
    }
}
