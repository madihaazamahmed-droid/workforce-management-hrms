package com.deepthought.workforce.controller;

import com.deepthought.workforce.dto.*;
import com.deepthought.workforce.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/clock-in")
    public ResponseEntity<AttendanceResponse> clockIn(@Valid @RequestBody ClockInRequest req) {
        return ResponseEntity.ok(attendanceService.clockIn(req));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<AttendanceResponse> clockOut(@Valid @RequestBody ClockOutRequest req) {
        return ResponseEntity.ok(attendanceService.clockOut(req));
    }

    /** Served exclusively from Redis */
    @GetMapping("/active")
    public ResponseEntity<List<ActiveWorkerDto>> getActiveWorkers() {
        return ResponseEntity.ok(attendanceService.getActiveWorkers());
    }

    @GetMapping("/log")
    public ResponseEntity<PagedResponse<AttendanceResponse>> getLog(
            @RequestParam Long workerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(attendanceService.getAttendanceLog(workerId, from, to, page, size));
    }
}
