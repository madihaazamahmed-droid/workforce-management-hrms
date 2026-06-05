package com.deepthought.workforce.service;

import com.deepthought.workforce.dto.*;
import com.deepthought.workforce.entity.AttendanceLog;
import com.deepthought.workforce.entity.OvertimeEntry;
import com.deepthought.workforce.entity.Site;
import com.deepthought.workforce.entity.Worker;
import com.deepthought.workforce.exception.WorkforceException;
import com.deepthought.workforce.repository.AttendanceRepository;
import com.deepthought.workforce.repository.OvertimeRepository;
import com.deepthought.workforce.repository.SiteRepository;
import com.deepthought.workforce.repository.WorkerRepository;
import com.deepthought.workforce.util.OvertimeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final WorkerRepository workerRepository;
    private final SiteRepository siteRepository;
    private final AttendanceRepository attendanceRepository;
    private final OvertimeRepository overtimeRepository;
    private final ActiveWorkerCacheService cacheService;

    @Transactional
    public AttendanceResponse clockIn(ClockInRequest req) {
        Worker worker = workerRepository.findById(req.getWorkerId())
                .orElseThrow(() -> WorkforceException.notFound("WORKER_NOT_FOUND",
                        "Worker not found: " + req.getWorkerId()));

        if (!worker.isActive()) {
            throw WorkforceException.badRequest("WORKER_INACTIVE", "Worker is not active");
        }

        Site site = siteRepository.findById(req.getSiteId())
                .orElseThrow(() -> WorkforceException.notFound("SITE_NOT_FOUND",
                        "Site not found: " + req.getSiteId()));

        if (!site.isActive()) {
            throw WorkforceException.badRequest("SITE_INACTIVE", "Site is not active");
        }

        LocalDateTime now = LocalDateTime.now();

        // Business rule: no clock-in if already clocked in
        attendanceRepository.findOpenAttendance(worker.getId()).ifPresent(existing -> {
            throw WorkforceException.conflict("DUPLICATE_CLOCK_IN",
                    "Worker is already clocked in at Site: " + existing.getSite().getSiteName());
        });

        AttendanceLog log = AttendanceLog.builder()
                .worker(worker)
                .site(site)
                .clockIn(now)
                .attendanceDate(now.toLocalDate())
                .build();

        log = attendanceRepository.save(log);

        // Add to Redis active workers cache
        cacheService.addActiveWorker(ActiveWorkerDto.builder()
                .workerId(worker.getId())
                .workerName(worker.getName())
                .designation(worker.getDesignation().name())
                .siteId(site.getId())
                .siteName(site.getSiteName())
                .siteLocation(site.getLocation())
                .clockInTime(now)
                .attendanceLogId(log.getId())
                .build());

        return mapToResponse(log);
    }

    @Transactional
    public AttendanceResponse clockOut(ClockOutRequest req) {
        Worker worker = workerRepository.findById(req.getWorkerId())
                .orElseThrow(() -> WorkforceException.notFound("WORKER_NOT_FOUND",
                        "Worker not found: " + req.getWorkerId()));

        AttendanceLog attendance = attendanceRepository.findOpenAttendance(worker.getId())
                .orElseThrow(() -> WorkforceException.badRequest("NOT_CLOCKED_IN",
                        "Worker is not currently clocked in"));

        LocalDateTime clockOut = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(attendance.getClockIn(), clockOut);
        BigDecimal totalHours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        boolean flagged = totalHours.compareTo(OvertimeCalculator.MAX_SHIFT_HOURS) > 0;
        if (flagged) {
            log.warn("Attendance flagged for review: workerId={}, hours={}", worker.getId(), totalHours);
        }

        // Get current month's OT so we can apply the 60-hour cap
        LocalDate today = clockOut.toLocalDate();
        BigDecimal monthOtSoFar = attendanceRepository.sumOvertimeHoursForMonth(
                worker.getId(), today.getYear(), today.getMonthValue());
        if (monthOtSoFar == null) monthOtSoFar = BigDecimal.ZERO;

        BigDecimal overtimeHours = OvertimeCalculator.calculateOvertimeHours(totalHours, monthOtSoFar);

        attendance.setClockOut(clockOut);
        attendance.setTotalHours(totalHours);
        attendance.setOvertimeHours(overtimeHours);
        attendance.setFlagged(flagged);
        attendance = attendanceRepository.save(attendance);

        // Create overtime entry if applicable
        if (overtimeHours.compareTo(BigDecimal.ZERO) > 0) {
            OvertimeCalculator.OvertimeCalcResult calc =
                    OvertimeCalculator.calculateAmount(overtimeHours, worker.getDailyWageRate());

            OvertimeEntry entry = OvertimeEntry.builder()
                    .worker(worker)
                    .attendance(attendance)
                    .entryDate(today)
                    .overtimeHours(overtimeHours)
                    .overtimeRateApplied(calc.effectiveRate())
                    .amount(calc.amount())
                    .build();
            overtimeRepository.save(entry);
        }

        // Remove from Redis
        cacheService.removeActiveWorker(worker.getId());

        return mapToResponse(attendance);
    }

    /** Served exclusively from Redis — no DB hit */
    public List<ActiveWorkerDto> getActiveWorkers() {
        return cacheService.getAllActiveWorkers();
    }

    @Transactional(readOnly = true)
    public PagedResponse<AttendanceResponse> getAttendanceLog(
            Long workerId, LocalDate from, LocalDate to, int page, int size) {

        workerRepository.findById(workerId)
                .orElseThrow(() -> WorkforceException.notFound("WORKER_NOT_FOUND",
                        "Worker not found: " + workerId));

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("clockIn").descending());
        Page<AttendanceLog> resultPage = attendanceRepository
                .findByWorkerAndDateRange(workerId, from, to, pageRequest);

        return PagedResponse.<AttendanceResponse>builder()
                .content(resultPage.getContent().stream().map(this::mapToResponse).toList())
                .currentPage(page)
                .totalPages(resultPage.getTotalPages())
                .totalElements(resultPage.getTotalElements())
                .pageSize(size)
                .last(resultPage.isLast())
                .build();
    }

    private AttendanceResponse mapToResponse(AttendanceLog a) {
        return AttendanceResponse.builder()
                .id(a.getId())
                .workerId(a.getWorker().getId())
                .workerName(a.getWorker().getName())
                .siteId(a.getSite().getId())
                .siteName(a.getSite().getSiteName())
                .clockIn(a.getClockIn())
                .clockOut(a.getClockOut())
                .totalHours(a.getTotalHours())
                .overtimeHours(a.getOvertimeHours())
                .attendanceDate(a.getAttendanceDate())
                .flagged(a.isFlagged())
                .build();
    }
}
