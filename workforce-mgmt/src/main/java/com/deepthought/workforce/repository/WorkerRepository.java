package com.deepthought.workforce.repository;

import com.deepthought.workforce.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
    Optional<Worker> findByPhone(String phone);
    boolean existsByPhone(String phone);
}
