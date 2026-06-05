package com.deepthought.workforce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClockOutRequest {
    @NotNull(message = "workerId is required")
    private Long workerId;
}
