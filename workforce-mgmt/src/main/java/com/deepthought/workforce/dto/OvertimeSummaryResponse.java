package com.deepthought.workforce.dto;

import com.deepthought.workforce.enums.SettlementStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class OvertimeSummaryResponse {
    private Long workerId;
    private String workerName;
    private String month; // YYYY-MM
    private BigDecimal totalOvertimeHours;
    private BigDecimal totalPayoutAmount;
    private SettlementStatus overallStatus; // PENDING if any entry is pending
    private List<OvertimeDayBreakdown> breakdown;

    @Data
    @Builder
    public static class OvertimeDayBreakdown {
        private LocalDate date;
        private BigDecimal overtimeHours;
        private BigDecimal rateApplied;
        private BigDecimal amount;
        private SettlementStatus status;
    }
}
