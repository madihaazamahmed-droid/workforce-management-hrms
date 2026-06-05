package com.deepthought.workforce.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceResponse {
    private Long id;
    private Long workerId;
    private String workerName;
    private Long siteId;
    private String siteName;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private BigDecimal totalHours;
    private BigDecimal overtimeHours;
    private LocalDate attendanceDate;
    private boolean flagged;
}
