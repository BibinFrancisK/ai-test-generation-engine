package com.testgen.analysis;

import com.testgen.model.DiffChangeType;
import com.testgen.model.DiffHunk;
import com.testgen.model.FileDiff;
import com.testgen.util.Constants;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DiffParser {

    private static final Pattern HUNK_HEADER = Pattern.compile(Constants.DIFF_HUNK_HEADER_REGEX);

    public List<FileDiff> parse(String diffText) {
        if (diffText == null || diffText.isBlank()) {
            return List.of();
        }

        List<FileDiff> result = new ArrayList<>();
        String[] lines = diffText.split("\n", -1);

        String currentPath = null;
        DiffChangeType currentChangeType = DiffChangeType.MODIFIED;
        boolean nextIsAdded = false;
        List<DiffHunk> currentHunks = new ArrayList<>();
        List<String> hunkLines = new ArrayList<>();
        String hunkFileName = null;
        int hunkStart = 0;
        int hunkCount = 1;
        boolean inHunk = false;

        for (String line : lines) {
            if (line.startsWith("diff --git ")) {
                // Flush previous file
                if (currentPath != null && currentPath.endsWith(".java")) {
                    flushHunk(hunkLines, hunkFileName, hunkStart, hunkCount, currentHunks, inHunk);
                    result.add(new FileDiff(currentPath, currentChangeType, List.copyOf(currentHunks)));
                }
                currentPath = null;
                currentChangeType = DiffChangeType.MODIFIED;
                nextIsAdded = false;
                currentHunks = new ArrayList<>();
                hunkLines = new ArrayList<>();
                inHunk = false;
                continue;
            }

            if (line.startsWith("--- ")) {
                nextIsAdded = line.equals(Constants.DIFF_OLD_FILE_MARKER);
                continue;
            }

            if (line.startsWith("+++ ")) {
                if (line.equals(Constants.DIFF_NEW_FILE_MARKER)) {
                    currentChangeType = DiffChangeType.DELETED;
                } else if (nextIsAdded) {
                    currentChangeType = DiffChangeType.ADDED;
                } else {
                    currentChangeType = DiffChangeType.MODIFIED;
                }
                // Extract path: "+++ b/src/..." → "src/..."
                String rawPath = line.substring(4);
                if (rawPath.startsWith("b/")) {
                    rawPath = rawPath.substring(2);
                } else if (rawPath.startsWith("a/")) {
                    rawPath = rawPath.substring(2);
                }
                currentPath = rawPath.trim();
                continue;
            }

            if (currentPath == null || !currentPath.endsWith(".java")) {
                continue;
            }

            Matcher hunkMatcher = HUNK_HEADER.matcher(line);
            if (hunkMatcher.find()) {
                // Flush previous hunk
                flushHunk(hunkLines, hunkFileName, hunkStart, hunkCount, currentHunks, inHunk);
                hunkStart = Integer.parseInt(hunkMatcher.group(1));
                String countStr = hunkMatcher.group(2);
                hunkCount = (countStr != null) ? Integer.parseInt(countStr) : 1;
                hunkFileName = currentPath;
                hunkLines = new ArrayList<>();
                inHunk = true;
                continue;
            }

            if (inHunk) {
                hunkLines.add(line);
            }
        }

        // Flush final file
        if (currentPath != null && currentPath.endsWith(".java")) {
            flushHunk(hunkLines, hunkFileName, hunkStart, hunkCount, currentHunks, inHunk);
            result.add(new FileDiff(currentPath, currentChangeType, List.copyOf(currentHunks)));
        }

        return List.copyOf(result);
    }

    private void flushHunk(List<String> lines, String fileName, int start, int count,
                            List<DiffHunk> target, boolean inHunk) {
        if (!inHunk || fileName == null) {
            return;
        }
        target.add(new DiffHunk(fileName, start, count, String.join("\n", lines)));
    }
}
