package com.deepthought.workforce.service;

import com.deepthought.workforce.entity.Worker;
import com.deepthought.workforce.entity.Site;
import com.deepthought.workforce.exception.WorkforceException;
import com.deepthought.workforce.repository.SiteRepository;
import com.deepthought.workforce.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final SiteRepository siteRepository;
    private final ActiveWorkerCacheService cacheService;

    @Transactional
    public Worker createWorker(Worker worker) {
        if (workerRepository.existsByPhone(worker.getPhone())) {
            throw WorkforceException.conflict("DUPLICATE_PHONE",
                    "Worker with phone " + worker.getPhone() + " already exists");
        }
        return workerRepository.save(worker);
    }

    @Transactional
    public Worker updateWorker(Long id, Worker update) {
        Worker existing = workerRepository.findById(id)
                .orElseThrow(() -> WorkforceException.notFound("WORKER_NOT_FOUND", "Worker not found: " + id));

        existing.setName(update.getName());
        existing.setDesignation(update.getDesignation());
        existing.setDailyWageRate(update.getDailyWageRate());
        existing.setActive(update.isActive());

        Worker saved = workerRepository.save(existing);

        // Invalidate Redis session — cached entry has stale name/designation/wage
        cacheService.invalidateWorkerSession(id);

        return saved;
    }

    @Transactional(readOnly = true)
    public Worker getWorker(Long id) {
        return workerRepository.findById(id)
                .orElseThrow(() -> WorkforceException.notFound("WORKER_NOT_FOUND", "Worker not found: " + id));
    }

    @Transactional
    public Site createSite(Site site) {
        return siteRepository.save(site);
    }
}
