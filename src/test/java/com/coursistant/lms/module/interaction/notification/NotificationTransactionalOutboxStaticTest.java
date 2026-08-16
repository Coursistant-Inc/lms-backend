package com.coursistant.lms.module.interaction.notification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTransactionalOutboxStaticTest {

    private static final List<String> FORBIDDEN = List.of(
            "publishInTransaction(",
            "outboxWriter.write(",
            "insertIgnoreDuplicate(",
            "recordSubmissionReceived(",
            "recordAssignmentPublished(",
            "recordGradesReleased(",
            "recordGradeCorrectedAfterRelease("
    );

    @Test
    void afterCommitLambdas_doNotWriteOutbox() throws Exception {
        Path root = Path.of("src/main/java");
        List<String> violations = new ArrayList<>();
        try (var walk = Files.walk(root)) {
            for (Path path : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                String src = Files.readString(path);
                int idx = 0;
                while ((idx = src.indexOf(".afterCommit(", idx)) >= 0) {
                    int open = src.indexOf('(', idx);
                    int close = matchingParen(src, open);
                    String arg = src.substring(open, close + 1);
                    idx = close + 1;
                    if (arg.contains("triggerFastPath") || arg.contains("cancelPendingEmailsFor")
                            || arg.contains("notifyDueDateChanged")) {
                        continue;
                    }
                    for (String forbidden : FORBIDDEN) {
                        if (arg.contains(forbidden)) {
                            violations.add(path + " afterCommit contains " + forbidden);
                        }
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), String.join("\n", violations));
    }

    @Test
    void assignmentDueDateAfterCommit_isLogOnly() throws Exception {
        String src = Files.readString(Path.of(
                "src/main/java/com/coursistant/lms/module/assignment/service/AssignmentService.java"));
        assertTrue(src.contains("notifyDueDateChanged"));
        int idx = src.indexOf("assignmentNotificationService.afterCommit(");
        assertTrue(idx > 0);
        int close = matchingParen(src, src.indexOf('(', idx + "assignmentNotificationService.afterCommit".length() - 1));
        String arg = src.substring(idx, close);
        assertTrue(arg.contains("notifyDueDateChanged"));
        assertFalse(arg.contains("publishInTransaction"));
    }

    private static int matchingParen(String src, int open) {
        int depth = 0;
        for (int i = open; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return src.length() - 1;
    }
}
