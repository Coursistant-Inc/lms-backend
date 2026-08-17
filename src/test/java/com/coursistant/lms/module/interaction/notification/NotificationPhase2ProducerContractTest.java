package com.coursistant.lms.module.interaction.notification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationPhase2ProducerContractTest {

    @Test
    void assignmentPublishAndPatch_useGuardedVersionsAndActualFieldCompare() throws Exception {
        String src = Files.readString(Path.of(
                "src/main/java/com/coursistant/lms/module/assignment/service/AssignmentService.java"));
        assertTrue(src.contains("publishAndIncrementPublicationVersion"));
        assertTrue(src.contains("recordAssignmentPublished(updated, userId)"));
        assertTrue(src.contains("incrementScheduleVersion"));
        assertTrue(src.contains("recordScheduleChanged(updated, userId)"));
        assertTrue(src.contains("existing.getDueAt(), updated.getDueAt()"));
        assertTrue(src.contains("existing.getLateUntil(), updated.getLateUntil()"));
        assertFalse(src.contains("QUIZ_EXTENSION_CHANGED"));
    }

    @Test
    void quizPublishAndPatch_emitDistinctScheduleAndTimeLimitEvents() throws Exception {
        String src = Files.readString(Path.of(
                "src/main/java/com/coursistant/lms/module/quiz/service/QuizAuthoringService.java"));
        assertTrue(src.contains("publishAndIncrementPublicationVersion"));
        assertTrue(src.contains("recordQuizPublished"));
        assertTrue(src.contains("recordScheduleChanged"));
        assertTrue(src.contains("recordTimeLimitChanged"));
        assertFalse(src.contains("QUIZ_EXTENSION_CHANGED"));
        assertFalse(src.toLowerCase().contains("personal extension"));
    }

    @Test
    void weekPublish_usesGuardedSql() throws Exception {
        String src = Files.readString(Path.of(
                "src/main/java/com/coursistant/lms/module/course/content/week/service/CourseWeekService.java"));
        assertTrue(src.contains("publishAndIncrementPublicationVersion"));
        assertTrue(src.contains("WEEK_PUBLISHED"));
        assertTrue(src.contains("if (published == 1)"));
    }

    @Test
    void groupMutations_notifyOnEverySuccessfulPath() throws Exception {
        String src = Files.readString(Path.of(
                "src/main/java/com/coursistant/lms/module/course/group/service/GroupMembershipService.java"));
        assertTrue(src.contains("notifyAdded"));
        assertTrue(src.contains("notifyRemoved"));
        assertTrue(src.contains("notifyMoved"));
        int join = src.indexOf("public MembershipMutationResponse join(");
        int leave = src.indexOf("public MembershipMutationResponse leave(");
        int switchGroup = src.indexOf("public MembershipMutationResponse switchGroup(");
        int distribute = src.indexOf("public List<MembershipResponse> distributeRandom(");
        int end = src.indexOf("public void endGroupMemberships(");
        assertTrue(src.substring(join, leave).contains("notifyAdded"));
        assertTrue(src.substring(leave, switchGroup).contains("notifyRemoved"));
        assertTrue(src.substring(switchGroup, distribute).contains("notifyMoved"));
        assertTrue(src.substring(distribute, end).contains("notifyAdded"));
        assertTrue(src.substring(end).contains("notifyRemoved"));
        assertTrue(src.substring(distribute, end).contains("membersByGroup"));
        assertFalse(src.substring(distribute, end).contains("List.of(studentId)"));
    }

    @Test
    void courseEventCreate_publishes_updateDoesNot() throws Exception {
        String src = Files.readString(Path.of(
                "src/main/java/com/coursistant/lms/module/course/event/service/CourseEventService.java"));
        int create = src.indexOf("public CourseEventResponse create(");
        int update = src.indexOf("public CourseEventResponse update(");
        int delete = src.indexOf("public void delete(");
        assertTrue(src.substring(create, update).contains("publishCreatedNotification"));
        assertFalse(src.substring(update, delete).contains("publishCreatedNotification"));
        int requireCourse = src.indexOf("private Course requireCourse", delete);
        assertFalse(src.substring(delete, requireCourse).contains("publishCreatedNotification"));
    }
}
