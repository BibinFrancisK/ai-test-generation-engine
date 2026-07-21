package com.testgen.api;

import com.testgen.healing.HealingOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class HealingController {

    private static final Logger log = LoggerFactory.getLogger(HealingController.class);

    private final HealingOrchestrator orchestrator;

    public HealingController(HealingOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/heal-tests")
    public ResponseEntity<HealingResponse> healTests(@RequestBody HealingRequest request) {
        try {
            HealingResponse response = orchestrator.orchestrate(request);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("Unhandled error orchestrating test healing for testRunId={}", request.testRunId(), e);
            HealingResponse failure = new HealingResponse(request.testRunId(), List.of(), Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(failure);
        }
    }
}
