package com.deepthought.workforce.repository;

import com.deepthought.workforce.entity.AttendanceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceLog, Long> {

    // Find open (clocked-in but not clocked-out) record for a worker
    @Query("SELECT a FROM AttendanceLog a WHERE a.worker.id = :workerId AND a.clockOut IS NULL")
    Optional<AttendanceLog> findOpenAttendance(@Param("workerId") Long workerId);

    // Paginated attendance log with JOIN FETCH — fixes N+1 (TICKET LF-203)
    @Query(value = "SELECT a FROM AttendanceLog a JOIN FETCH a.worker w JOIN FETCH a.site s " +
                   "WHERE a.worker.id = :workerId " +
                   "AND a.attendanceDate BETWEEN :from AND :to",
           countQuery = "SELECT COUNT(a) FROM AttendanceLog a WHERE a.worker.id = :workerId " +
                        "AND a.attendanceDate BETWEEN :from AND :to")
    Page<AttendanceLog> findByWorkerAndDateRange(
            @Param("workerId") Long workerId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    // Total overtime hours for a worker in a given month (used for 60-hour cap check)
    @Query("SELECT COALESCE(SUM(a.overtimeHours), 0) FROM AttendanceLog a " +
           "WHERE a.worker.id = :workerId " +
           "AND YEAR(a.attendanceDate) = :year AND MONTH(a.attendanceDate) = :month")
    java.math.BigDecimal sumOvertimeHoursForMonth(
            @Param("workerId") Long workerId,
            @Param("year") int year,
            @Param("month") int month);
}
