package com.testgen.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testgen.orchestration.TestGenerationOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TestGenerationControllerTest {

    @Mock
    private TestGenerationOrchestrator orchestrator;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestGenerationController(orchestrator)).build();
    }

    @Test
    void returns201OnSuccessfulValidation() throws Exception {
        TestGenerationResponse response = new TestGenerationResponse(
                "run-1", "class FooTest {}", "SUCCESS", List.of(), "s3://bucket/key", null, Instant.now());
        when(orchestrator.orchestrate(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/generate-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.testRunId").value("run-1"))
                .andExpect(jsonPath("$.validationStatus").value("SUCCESS"));
    }

    @Test
    void returns422OnCompileValidationFailure() throws Exception {
        TestGenerationResponse response = new TestGenerationResponse(
                "run-2", "class FooTest {}", "COMPILE_FAILED", List.of("cannot find symbol"),
                "s3://bucket/key", null, Instant.now());
        when(orchestrator.orchestrate(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/generate-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.testRunId").value("run-2"))
                .andExpect(jsonPath("$.validationErrors[0]").value("cannot find symbol"));
    }

    @Test
    void returns500WhenOrchestratorReportsInfraFailure() throws Exception {
        TestGenerationResponse response = new TestGenerationResponse(
                "run-3", null, "FAILED", List.of(), null, null, Instant.now());
        when(orchestrator.orchestrate(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/generate-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.testRunId").value("run-3"));
    }

    @Test
    void returns500WithGeneratedTestRunIdWhenOrchestratorThrows() throws Exception {
        when(orchestrator.orchestrate(any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/v1/generate-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.testRunId").isNotEmpty());
    }

    private TestGenerationRequest sampleRequest() {
        return new TestGenerationRequest(
                "owner", "repo", "main", "repo-1", "pr-1", "diff", "src/main/java/Foo.java", null);
    }
}
