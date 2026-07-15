package com.testgen.api;

import com.testgen.orchestration.TestGenerationOrchestrator;
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
import java.util.UUID;

import static com.testgen.util.Constants.STATUS_FAILED;
import static com.testgen.util.Constants.STATUS_SUCCESS;

@RestController
@RequestMapping("/api/v1")
public class TestGenerationController {

    private static final Logger log = LoggerFactory.getLogger(TestGenerationController.class);

    private final TestGenerationOrchestrator orchestrator;

    public TestGenerationController(TestGenerationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/generate-tests")
    public ResponseEntity<TestGenerationResponse> generateTests(@RequestBody TestGenerationRequest request) {
        TestGenerationResponse response;
        try {
            response = orchestrator.orchestrate(request);
        } catch (Exception e) {
            log.error("Unhandled error orchestrating test generation for repositoryId={}, pullRequestId={}",
                    request.repositoryId(), request.pullRequestId(), e);
            TestGenerationResponse failure = new TestGenerationResponse(
                    UUID.randomUUID().toString(), null, STATUS_FAILED, List.of(), null, null, Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(failure);
        }

        return ResponseEntity.status(statusCodeFor(response)).body(response);
    }

    private HttpStatus statusCodeFor(TestGenerationResponse response) {
        if (STATUS_SUCCESS.equals(response.validationStatus())) {
            return HttpStatus.CREATED;
        }
        if (STATUS_FAILED.equals(response.validationStatus())) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
