package com.deepthought.workforce.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Overtime calculation rules:
 * - Standard shift = 8 hours
 * - Hours 9-10 (first 2 OT hours): 1.5x daily wage rate
 * - Hours 11+ (beyond 2 OT hours): 2.0x daily wage rate
 * - Daily wage rate is per 8-hour shift, so hourly = dailyRate / 8
 */
public class OvertimeCalculator {

    private static final BigDecimal STANDARD_HOURS = BigDecimal.valueOf(8);
    private static final BigDecimal TIER_1_HOURS = BigDecimal.valueOf(2); // first 2 OT hours
    private static final BigDecimal RATE_TIER_1 = BigDecimal.valueOf(1.5);
    private static final BigDecimal RATE_TIER_2 = BigDecimal.valueOf(2.0);
    public static final BigDecimal MONTHLY_OT_CAP = BigDecimal.valueOf(60);
    public static final BigDecimal MAX_SHIFT_HOURS = BigDecimal.valueOf(16);

    /**
     * Calculate overtime hours for a shift, respecting monthly cap.
     *
     * @param totalHours        total hours worked in this shift
     * @param monthlyOtSoFar    overtime hours already accumulated this month
     * @return overtime hours to record (capped at remaining monthly allowance)
     */
    public static BigDecimal calculateOvertimeHours(BigDecimal totalHours, BigDecimal monthlyOtSoFar) {
        if (totalHours.compareTo(STANDARD_HOURS) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rawOvertimeHours = totalHours.subtract(STANDARD_HOURS);

        // Apply monthly cap
        BigDecimal remainingCap = MONTHLY_OT_CAP.subtract(monthlyOtSoFar);
        if (remainingCap.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO; // Already at cap
        }

        return rawOvertimeHours.min(remainingCap).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate overtime amount. Uses weighted average rate if span crosses tiers.
     * Returns a record with amount and effective rate for audit trail.
     */
    public static OvertimeCalcResult calculateAmount(BigDecimal overtimeHours, BigDecimal dailyWageRate) {
        if (overtimeHours.compareTo(BigDecimal.ZERO) <= 0) {
            return new OvertimeCalcResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal hourlyRate = dailyWageRate.divide(STANDARD_HOURS, 4, RoundingMode.HALF_UP);
        BigDecimal amount;
        BigDecimal effectiveRate;

        if (overtimeHours.compareTo(TIER_1_HOURS) <= 0) {
            // All hours in tier 1 (1.5x)
            amount = hourlyRate.multiply(RATE_TIER_1).multiply(overtimeHours);
            effectiveRate = RATE_TIER_1;
        } else {
            // Split: first 2 hours at 1.5x, rest at 2.0x
            BigDecimal tier1Amount = hourlyRate.multiply(RATE_TIER_1).multiply(TIER_1_HOURS);
            BigDecimal tier2Hours = overtimeHours.subtract(TIER_1_HOURS);
            BigDecimal tier2Amount = hourlyRate.multiply(RATE_TIER_2).multiply(tier2Hours);
            amount = tier1Amount.add(tier2Amount);
            // Weighted average rate for the record
            effectiveRate = amount.divide(hourlyRate.multiply(overtimeHours), 4, RoundingMode.HALF_UP);
        }

        return new OvertimeCalcResult(
                amount.setScale(2, RoundingMode.HALF_UP),
                effectiveRate.setScale(4, RoundingMode.HALF_UP),
                hourlyRate.setScale(2, RoundingMode.HALF_UP)
        );
    }

    public record OvertimeCalcResult(BigDecimal amount, BigDecimal effectiveRate, BigDecimal hourlyRate) {}
}
