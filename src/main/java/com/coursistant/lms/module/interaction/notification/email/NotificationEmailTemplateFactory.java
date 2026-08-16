package com.coursistant.lms.module.interaction.notification.email;

import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationEmailTemplateFactory {

    @Resource
    private NotificationProperties notificationProperties;

    public RenderedEmail renderImmediate(NotificationType type, Map<String, String> vars) {
        Map<String, String> clean = sanitize(vars);
        String courseCode = value(clean, "courseCode", "COURSE");
        String courseTitle = value(clean, "courseTitle", courseCode);
        String title = first(clean, "assignmentTitle", "quizTitle", "title");
        String deepLink = absoluteLink(clean.get("deepLink"));
        String submittedAt = value(clean, "submittedAt", "");
        return switch (type) {
            case ASSIGNMENT_SUBMISSION_RECEIVED -> new RenderedEmail(
                    "[" + courseCode + "] Submission received: " + title,
                    body(courseCode, courseTitle,
                            "Your submission was received: " + title + " at " + submittedAt + ".",
                            deepLink));
            case ASSIGNMENT_GRADE_RELEASED -> new RenderedEmail(
                    "[" + courseCode + "] Your assignment grade is available",
                    body(courseCode, courseTitle,
                            "Your assignment grade has been released: " + title + ".",
                            deepLink));
            case ASSIGNMENT_GRADE_CORRECTED -> new RenderedEmail(
                    "[" + courseCode + "] Your assignment grade was updated",
                    body(courseCode, courseTitle,
                            "Your assignment grade has been updated: " + title + ".",
                            deepLink));
            case QUIZ_GRADE_RELEASED -> new RenderedEmail(
                    "[" + courseCode + "] Your quiz grade is available",
                    body(courseCode, courseTitle,
                            "Your quiz grade has been released: " + title + ".",
                            deepLink));
            case QUIZ_GRADE_CORRECTED -> new RenderedEmail(
                    "[" + courseCode + "] Your quiz grade was updated",
                    body(courseCode, courseTitle,
                            "Your quiz grade has been updated: " + title + ".",
                            deepLink));
            default -> throw new IllegalArgumentException("No immediate email template for " + type);
        };
    }

    public RenderedEmail renderDigest(LocalDate digestDate, List<DigestCourseGroup> groups) {
        String date = digestDate == null ? "" : digestDate.toString();
        StringBuilder body = new StringBuilder();
        body.append("Your xLearn daily digest for ").append(date).append(".\n");
        if (groups != null) {
            for (DigestCourseGroup group : groups) {
                body.append("\n").append(sanitizeText(group.courseCode()))
                        .append(" — ").append(sanitizeText(group.courseTitle())).append("\n");
                if (group.lines() != null) {
                    for (String line : group.lines()) {
                        body.append("- ").append(sanitizeText(line)).append("\n");
                    }
                }
            }
        }
        return new RenderedEmail("[xLearn] Your daily digest for " + date, body.toString().trim());
    }

    public record DigestCourseGroup(String courseCode, String courseTitle, List<String> lines) {
    }

    private String body(String courseCode, String courseTitle, String event, String link) {
        return "Course: " + courseCode + " — " + courseTitle + "\n"
                + event + "\n"
                + "Open: " + link + "\n";
    }

    private String absoluteLink(String deepLink) {
        String base = notificationProperties.getEmail().getBaseUrl();
        if (base == null) {
            base = "";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String path = deepLink == null ? "" : sanitizeText(deepLink);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    private Map<String, String> sanitize(Map<String, String> vars) {
        Map<String, String> clean = new LinkedHashMap<>();
        if (vars == null) {
            return clean;
        }
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            clean.put(entry.getKey(), sanitizeText(entry.getValue()));
        }
        return clean;
    }

    public static String sanitizeText(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\n' || Character.isISOControl(c)) {
                out.append(' ');
            } else {
                out.append(c);
            }
        }
        String trimmed = out.toString().trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }

    private String value(Map<String, String> vars, String key, String fallback) {
        String v = vars.get(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    private String first(Map<String, String> vars, String... keys) {
        for (String key : keys) {
            String v = vars.get(key);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "Item";
    }
}
