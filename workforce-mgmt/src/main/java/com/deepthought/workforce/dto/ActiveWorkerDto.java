package com.deepthought.workforce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveWorkerDto implements Serializable {
    private Long workerId;
    private String workerName;
    private String designation;
    private Long siteId;
    private String siteName;
    private String siteLocation;
    private LocalDateTime clockInTime;
    private Long attendanceLogId;
}
