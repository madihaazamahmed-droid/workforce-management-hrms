package com.deepthought.workforce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_logs", indexes = {
        @Index(name = "idx_attendance_worker", columnList = "worker_id"),
        @Index(name = "idx_attendance_site", columnList = "site_id"),
        @Index(name = "idx_attendance_clock_in", columnList = "clock_in"),
        @Index(name = "idx_attendance_worker_date", columnList = "worker_id, attendance_date"),
        @Index(name = "idx_attendance_flagged", columnList = "flagged")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "clock_in", nullable = false)
    private LocalDateTime clockIn;

    @Column(name = "clock_out")
    private LocalDateTime clockOut;

    @Column(name = "total_hours", precision = 5, scale = 2)
    private BigDecimal totalHours;

    @Column(name = "overtime_hours", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    // True if shift exceeded 16 hours — needs review
    @Column(nullable = false)
    @Builder.Default
    private boolean flagged = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
