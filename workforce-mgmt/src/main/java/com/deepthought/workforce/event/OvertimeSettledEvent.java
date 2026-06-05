package com.deepthought.workforce.event;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class OvertimeSettledEvent {
    private final Long workerId;
    private final String workerName;
    private final String month;
    private final BigDecimal totalAmount;

    public OvertimeSettledEvent(Long workerId, String workerName, String month, BigDecimal totalAmount) {
        this.workerId = workerId;
        this.workerName = workerName;
        this.month = month;
        this.totalAmount = totalAmount;
    }
}
