package com.deepthought.workforce.entity;

import com.deepthought.workforce.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "overtime_entries", indexes = {
        @Index(name = "idx_overtime_worker", columnList = "worker_id"),
        @Index(name = "idx_overtime_attendance", columnList = "attendance_id"),
        @Index(name = "idx_overtime_worker_date", columnList = "worker_id, entry_date"),
        @Index(name = "idx_overtime_settlement", columnList = "worker_id, settlement_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", nullable = false)
    private AttendanceLog attendance;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "overtime_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal overtimeHours;

    @Column(name = "overtime_rate_applied", nullable = false, precision = 5, scale = 2)
    private BigDecimal overtimeRateApplied;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", nullable = false, length = 10)
    @Builder.Default
    private SettlementStatus settlementStatus = SettlementStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
