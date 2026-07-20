package com.testgen.healing;

import com.testgen.model.ChangedMethod;
import com.testgen.model.TestFailure;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HealingTrigger {

    public boolean shouldHeal(TestFailure failure, List<ChangedMethod> correlatedChanges) {
        return correlatedChanges != null && !correlatedChanges.isEmpty();
    }
}
