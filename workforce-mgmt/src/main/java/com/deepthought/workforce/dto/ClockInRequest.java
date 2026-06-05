package com.deepthought.workforce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClockInRequest {
    @NotNull(message = "workerId is required")
    private Long workerId;

    @NotNull(message = "siteId is required")
    private Long siteId;
}
