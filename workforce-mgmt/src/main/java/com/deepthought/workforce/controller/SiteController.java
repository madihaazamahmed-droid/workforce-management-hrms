package com.deepthought.workforce.controller;

import com.deepthought.workforce.entity.Site;
import com.deepthought.workforce.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

    private final WorkerService workerService;

    @PostMapping
    public ResponseEntity<Site> create(@RequestBody Site site) {
        return ResponseEntity.ok(workerService.createSite(site));
    }
}
