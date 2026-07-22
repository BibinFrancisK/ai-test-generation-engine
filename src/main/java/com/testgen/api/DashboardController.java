package com.testgen.api;

import com.testgen.dashboard.CoverageAggregator;
import com.testgen.model.CoverageStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final CoverageAggregator aggregator;

    public DashboardController(CoverageAggregator aggregator) {
        this.aggregator = aggregator;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<CoverageStats> dashboard(@RequestParam String repositoryId) {
        try {
            return ResponseEntity.ok(aggregator.aggregate(repositoryId));
        } catch (Exception e) {
            log.error("Unhandled error aggregating coverage stats for repositoryId={}", repositoryId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
