package com.testgen.api;

import com.testgen.dashboard.CoverageAggregator;
import com.testgen.model.CoverageStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private CoverageAggregator aggregator;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DashboardController(aggregator)).build();
    }

    @Test
    void returns200WithCoverageStats() throws Exception {
        CoverageStats stats = new CoverageStats(10, 8, 6, 5, 3, 2, 0.6);
        when(aggregator.aggregate(eq("owner/repo"))).thenReturn(stats);

        mockMvc.perform(get("/api/v1/dashboard").param("repositoryId", "owner/repo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTestRuns").value(10))
                .andExpect(jsonPath("$.aiGeneratedTests").value(8))
                .andExpect(jsonPath("$.compilePassed").value(6))
                .andExpect(jsonPath("$.executionPassed").value(5))
                .andExpect(jsonPath("$.selfHealingAttempts").value(3))
                .andExpect(jsonPath("$.selfHealingSuccesses").value(2))
                .andExpect(jsonPath("$.passRate").value(0.6));
    }

    @Test
    void returns200WithAllZeroStatsWhenNoDataFound() throws Exception {
        CoverageStats emptyStats = new CoverageStats(0, 0, 0, 0, 0, 0, 0.0);
        when(aggregator.aggregate(eq("unknown/repo"))).thenReturn(emptyStats);

        mockMvc.perform(get("/api/v1/dashboard").param("repositoryId", "unknown/repo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTestRuns").value(0));
    }

    @Test
    void returns500WhenAggregatorThrows() throws Exception {
        when(aggregator.aggregate(eq("owner/repo"))).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/dashboard").param("repositoryId", "owner/repo"))
                .andExpect(status().isInternalServerError());
    }
}
