package com.testgen.healing;

import com.testgen.generation.NoopProvider;
import com.testgen.model.HealingResult;
import com.testgen.model.TestFailure;
import com.testgen.model.ValidationResult;
import com.testgen.model.ValidationStage;
import com.testgen.validation.TestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestHealerTest {

    private static final String FAILING_TEST_CODE = "class SampleServiceTest { }";
    private static final String CLASS_UNDER_TEST_SOURCE = "class SampleService { }";
    private static final String CLASS_UNDER_TEST_CLASS_NAME = "SampleService";
    private static final String VALID_TEST_CODE = "package com.example.service; class SampleServiceTest { }";

    // Spy on a concrete permitted type — Mockito cannot mock sealed interfaces directly
    @Spy
    private NoopProvider llmProvider;

    @Mock
    private HealingPromptBuilder promptBuilder;

    @Mock
    private TestValidator testValidator;

    private TestHealer healer;

    private final TestFailure failure = new TestFailure(
            "com.example.service.SampleServiceTest", "processOrderReturnsFormattedString",
            "expected: <ord-001:5> but was: <ord-001-5>", "stack trace");

    @BeforeEach
    void setUp() {
        healer = new TestHealer(llmProvider, promptBuilder, testValidator);
    }

    @Test
    void returnsHealingSuccessWhenFirstAttemptPassesValidation() {
        ValidationResult.ValidationSuccess success = new ValidationResult.ValidationSuccess(
                "SampleServiceTest", List.of("processOrderReturnsFormattedString"), 1);

        when(promptBuilder.buildSystemPrompt()).thenReturn("system prompt");
        when(promptBuilder.buildUserPrompt(any(), any(), any())).thenReturn("user prompt");
        doReturn(VALID_TEST_CODE).when(llmProvider).generate(any(), any());
        when(testValidator.validate(any(), any(), any())).thenReturn(success);

        HealingResult result = healer.heal(FAILING_TEST_CODE, CLASS_UNDER_TEST_SOURCE, CLASS_UNDER_TEST_CLASS_NAME, failure);

        assertThat(result).isInstanceOf(HealingResult.HealingSuccess.class);
        HealingResult.HealingSuccess healingSuccess = (HealingResult.HealingSuccess) result;
        assertThat(healingSuccess.fixedTest().testCode()).isEqualTo(VALID_TEST_CODE);
        assertThat(healingSuccess.fixedTest().className()).isEqualTo("SampleServiceTest");
        assertThat(healingSuccess.fixedTest().packageName()).isEqualTo("com.example.service");
        verify(llmProvider, times(1)).generate(any(), any());
    }

    @Test
    void retriesOnceThenSucceedsWhenFirstAttemptFailsValidation() {
        ValidationResult.ValidationFailure firstFailure = new ValidationResult.ValidationFailure(
                "SampleServiceTest", List.of("expected true but was false"), ValidationStage.EXECUTE);
        ValidationResult.ValidationSuccess secondSuccess = new ValidationResult.ValidationSuccess(
                "SampleServiceTest", List.of("processOrderReturnsFormattedString"), 1);

        when(promptBuilder.buildSystemPrompt()).thenReturn("system prompt");
        when(promptBuilder.buildUserPrompt(any(), any(), any())).thenReturn("user prompt");
        when(promptBuilder.buildRetryUserPrompt(any(), any(), any(), any())).thenReturn("retry prompt");
        doReturn(VALID_TEST_CODE).when(llmProvider).generate(any(), any());
        when(testValidator.validate(any(), any(), any())).thenReturn(firstFailure, secondSuccess);

        HealingResult result = healer.heal(FAILING_TEST_CODE, CLASS_UNDER_TEST_SOURCE, CLASS_UNDER_TEST_CLASS_NAME, failure);

        assertThat(result).isInstanceOf(HealingResult.HealingSuccess.class);
        verify(llmProvider, times(2)).generate(any(), any());
        verify(promptBuilder).buildRetryUserPrompt(
                eq(FAILING_TEST_CODE), eq(CLASS_UNDER_TEST_SOURCE), eq(failure), eq(firstFailure.errors()));
    }

    @Test
    void returnsHealingFailureWhenRetryAlsoFailsValidation() {
        ValidationResult.ValidationFailure firstFailure = new ValidationResult.ValidationFailure(
                "SampleServiceTest", List.of("compile error A"), ValidationStage.COMPILE);
        ValidationResult.ValidationFailure secondFailure = new ValidationResult.ValidationFailure(
                "SampleServiceTest", List.of("compile error B"), ValidationStage.COMPILE);

        when(promptBuilder.buildSystemPrompt()).thenReturn("system prompt");
        when(promptBuilder.buildUserPrompt(any(), any(), any())).thenReturn("user prompt");
        when(promptBuilder.buildRetryUserPrompt(any(), any(), any(), any())).thenReturn("retry prompt");
        doReturn(VALID_TEST_CODE).when(llmProvider).generate(any(), any());
        when(testValidator.validate(any(), any(), any())).thenReturn(firstFailure, secondFailure);

        HealingResult result = healer.heal(FAILING_TEST_CODE, CLASS_UNDER_TEST_SOURCE, CLASS_UNDER_TEST_CLASS_NAME, failure);

        assertThat(result).isInstanceOf(HealingResult.HealingFailure.class);
        HealingResult.HealingFailure healingFailure = (HealingResult.HealingFailure) result;
        assertThat(healingFailure.validationErrors()).isEqualTo(secondFailure.errors());
        verify(llmProvider, times(2)).generate(any(), any());
    }
}
