package com.deepthought.workforce.controller;

import com.deepthought.workforce.entity.Site;
import com.deepthought.workforce.entity.Worker;
import com.deepthought.workforce.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    @PostMapping
    public ResponseEntity<Worker> create(@RequestBody Worker worker) {
        return ResponseEntity.ok(workerService.createWorker(worker));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Worker> get(@PathVariable Long id) {
        return ResponseEntity.ok(workerService.getWorker(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Worker> update(@PathVariable Long id, @RequestBody Worker worker) {
        return ResponseEntity.ok(workerService.updateWorker(id, worker));
    }
}

