package com.testgen.analysis;

import com.testgen.model.ChangedMethod;
import com.testgen.model.DiffChangeType;
import com.testgen.model.DiffHunk;
import com.testgen.model.FileDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DiffAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(DiffAnalyzer.class);

    private final SourceAnalyzer sourceAnalyzer;

    public DiffAnalyzer(SourceAnalyzer sourceAnalyzer) {
        this.sourceAnalyzer = sourceAnalyzer;
    }

    public List<ChangedMethod> analyze(List<FileDiff> fileDiffs, Map<String, String> sourceByPath) {
        if (fileDiffs == null || fileDiffs.isEmpty()) {
            return List.of();
        }

        List<ChangedMethod> result = new ArrayList<>();

        for (FileDiff fileDiff : fileDiffs) {
            if (DiffChangeType.DELETED == fileDiff.changeType()) {
                continue;
            }

            String source = sourceByPath.get(fileDiff.filePath());
            if (source == null) {
                log.debug("No source found for path '{}', skipping", fileDiff.filePath());
                continue;
            }

            List<ChangedMethod> allMethods = sourceAnalyzer.analyze(source);
            List<DiffHunk> hunks = fileDiff.hunks();

            for (ChangedMethod method : allMethods) {
                if (overlapsAnyHunk(method.startLine(), method.endLine(), hunks)) {
                    result.add(method);
                }
            }
        }

        return List.copyOf(result);
    }

    private boolean overlapsAnyHunk(int methodStart, int methodEnd, List<DiffHunk> hunks) {
        for (DiffHunk hunk : hunks) {
            int hunkStart = hunk.addedStartLine();
            int hunkEnd = hunkStart + hunk.addedLineCount() - 1;
            if (methodStart <= hunkEnd && methodEnd >= hunkStart) {
                return true;
            }
        }
        return false;
    }
}
