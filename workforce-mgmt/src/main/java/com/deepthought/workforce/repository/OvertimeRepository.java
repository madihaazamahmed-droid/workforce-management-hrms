package com.deepthought.workforce.repository;

import com.deepthought.workforce.entity.OvertimeEntry;
import com.deepthought.workforce.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OvertimeRepository extends JpaRepository<OvertimeEntry, Long> {

    @Query("SELECT o FROM OvertimeEntry o JOIN FETCH o.worker w JOIN FETCH o.attendance a " +
           "WHERE o.worker.id = :workerId " +
           "AND YEAR(o.entryDate) = :year AND MONTH(o.entryDate) = :month")
    List<OvertimeEntry> findByWorkerAndMonth(
            @Param("workerId") Long workerId,
            @Param("year") int year,
            @Param("month") int month);

    @Query("SELECT o FROM OvertimeEntry o WHERE o.worker.id = :workerId " +
           "AND YEAR(o.entryDate) = :year AND MONTH(o.entryDate) = :month " +
           "AND o.settlementStatus = :status")
    List<OvertimeEntry> findByWorkerMonthAndStatus(
            @Param("workerId") Long workerId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("status") SettlementStatus status);
}
