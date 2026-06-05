package com.deepthought.workforce.controller;

import com.deepthought.workforce.dto.OvertimeSummaryResponse;
import com.deepthought.workforce.dto.SettlementResponse;
import com.deepthought.workforce.service.OvertimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/overtime")
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;

    @GetMapping("/summary/{workerId}")
    public ResponseEntity<OvertimeSummaryResponse> getSummary(
            @PathVariable Long workerId,
            @RequestParam String month) {
        return ResponseEntity.ok(overtimeService.getSummary(workerId, month));
    }

    @PostMapping("/settle/{workerId}")
    public ResponseEntity<SettlementResponse> settle(
            @PathVariable Long workerId,
            @RequestParam String month) {
        return ResponseEntity.ok(overtimeService.settle(workerId, month));
    }
}
