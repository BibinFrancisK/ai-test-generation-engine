package com.testgen.healing;

import com.testgen.model.ChangedMethod;
import com.testgen.model.TestFailure;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeCorrelatorTest {

    private final ChangeCorrelator correlator = new ChangeCorrelator();
    private final HealingTrigger healingTrigger = new HealingTrigger();

    @Test
    void correlatesFailureToChangeInSameClassAndTriggersHealing() {
        TestFailure failure = new TestFailure(
                "com.example.service.SampleServiceTest", "processOrderReturnsFormattedString",
                "expected: <ord-001:5> but was: <ord-001-5>", "stack trace");
        ChangedMethod changedMethod = new ChangedMethod(
                "SampleService", "processOrder", List.of("String", "int"), "String", List.of(), 20, 22);

        Map<TestFailure, List<ChangedMethod>> correlated = correlator.correlate(List.of(failure), List.of(changedMethod));

        assertThat(correlated.get(failure)).containsExactly(changedMethod);
        assertThat(healingTrigger.shouldHeal(failure, correlated.get(failure))).isTrue();
    }

    @Test
    void doesNotCorrelateFailureToUnrelatedClassAndSkipsHealing() {
        TestFailure failure = new TestFailure(
                "com.example.service.SampleServiceTest", "processOrderReturnsFormattedString",
                "expected: <ord-001:5> but was: <ord-001-5>", "stack trace");
        ChangedMethod unrelatedMethod = new ChangedMethod(
                "OrderRepository", "save", List.of("Order"), "void", List.of(), 10, 12);

        Map<TestFailure, List<ChangedMethod>> correlated = correlator.correlate(List.of(failure), List.of(unrelatedMethod));

        assertThat(correlated.get(failure)).isEmpty();
        assertThat(healingTrigger.shouldHeal(failure, correlated.get(failure))).isFalse();
    }
}
