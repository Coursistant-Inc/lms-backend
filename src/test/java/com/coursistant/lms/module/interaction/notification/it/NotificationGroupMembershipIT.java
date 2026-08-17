package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.group.entity.GroupMembershipAudit;
import com.coursistant.lms.module.course.group.service.GroupNotificationService;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase2SpringITBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationGroupMembershipIT extends NotificationPhase2SpringITBase {

    @Autowired private GroupNotificationService groupNotificationService;
    @Autowired private CourseMapper courseMapper;

    @Test
    void addRemoveMove_writeDisjointAudienceVariants() {
        int instructorId = insertInstructor();
        int target = insertUser("g-t-" + uuid() + "@example.com", true, "ACTIVE");
        int memberA = insertUser("g-a-" + uuid() + "@example.com", true, "ACTIVE");
        int memberB = insertUser("g-b-" + uuid() + "@example.com", true, "ACTIVE");
        int courseId = insertCourse(instructorId);
        Course course = courseMapper.selectById(courseId);
        int groupSetId = insertGroupSet(courseId);
        int oldGroup = insertGroup(courseId, groupSetId, "Old");
        int newGroup = insertGroup(courseId, groupSetId, "New");

        transactionTemplate.executeWithoutResult(status ->
                groupNotificationService.notifyAdded(course, groupSetId, oldGroup, target,
                        GroupMembershipAudit.ACTOR_USER, instructorId, 101,
                        List.of(target, memberA, memberB, instructorId)));
        assertEquals(2, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'GROUP_MEMBER_ADDED' AND event_key LIKE 'group-membership-change:101:%'
                """));
        assertDisjoint("group-membership-change:101:added:target", "group-membership-change:101:added:members");
        assertEquals(List.of(target), recipientsOf("group-membership-change:101:added:target"));
        List<Integer> addedMembers = recipientsOf("group-membership-change:101:added:members");
        assertEquals(Set.of(memberA, memberB), new HashSet<>(addedMembers));

        transactionTemplate.executeWithoutResult(status ->
                groupNotificationService.notifyRemoved(course, groupSetId, oldGroup, target,
                        GroupMembershipAudit.ACTOR_USER, instructorId, 102, List.of(memberA)));
        assertEquals(List.of(target), recipientsOf("group-membership-change:102:removed:target"));
        assertEquals(List.of(memberA), recipientsOf("group-membership-change:102:removed:members"));

        transactionTemplate.executeWithoutResult(status ->
                groupNotificationService.notifyMoved(course, groupSetId, oldGroup, newGroup, target,
                        GroupMembershipAudit.ACTOR_USER, target, 103,
                        List.of(memberA), List.of(target, memberB)));
        assertEquals(3, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'GROUP_MEMBER_MOVED' AND event_key LIKE 'group-membership-change:103:%'
                """));
        assertEquals(List.of(target), recipientsOf("group-membership-change:103:moved:target"));
        assertEquals(List.of(memberA), recipientsOf("group-membership-change:103:moved:old-members"));
        assertEquals(List.of(memberB), recipientsOf("group-membership-change:103:moved:new-members"));
        String message = jdbcTemplate.queryForObject(
                "SELECT message FROM notification_event_outbox WHERE event_key = 'group-membership-change:103:moved:target'",
                String.class);
        assertTrue(message.contains("Old"));
        assertTrue(message.contains("New"));
        assertEquals("GROUP_SET", jdbcTemplate.queryForObject(
                "SELECT subject_type FROM notification_event_outbox WHERE event_key = 'group-membership-change:103:moved:target'",
                String.class));
        assertEquals(groupSetId, jdbcTemplate.queryForObject(
                "SELECT subject_id FROM notification_event_outbox WHERE event_key = 'group-membership-change:103:moved:target'",
                Integer.class));
    }

    @Test
    void emptyMemberVariant_isSkipped_andAdminActorIsNotExcluded() {
        int instructorId = insertInstructor();
        int target = insertUser("g-solo-" + uuid() + "@example.com", true, "ACTIVE");
        int member = insertUser("g-keep-" + uuid() + "@example.com", true, "ACTIVE");
        int courseId = insertCourse(instructorId);
        Course course = courseMapper.selectById(courseId);
        int groupSetId = insertGroupSet(courseId);
        int groupId = insertGroup(courseId, groupSetId, "Alpha");

        transactionTemplate.executeWithoutResult(status ->
                groupNotificationService.notifyAdded(course, groupSetId, groupId, target,
                        GroupMembershipAudit.ACTOR_USER, instructorId, 201, List.of(target)));
        assertEquals(1, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE event_key LIKE 'group-membership-change:201:%'
                """));

        transactionTemplate.executeWithoutResult(status ->
                groupNotificationService.notifyAdded(course, groupSetId, groupId, target,
                        GroupMembershipAudit.ACTOR_ADMIN, 1, 202, List.of(target, member, 1)));
        List<Integer> members = recipientsOf("group-membership-change:202:added:members");
        assertTrue(members.contains(member));
        assertTrue(members.contains(1));
    }

    @Test
    void distributeRandomSource_keepsFullAudienceNotSelfOnly() throws Exception {
        String src = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/coursistant/lms/module/course/group/service/GroupMembershipService.java"));
        int start = src.indexOf("public List<MembershipResponse> distributeRandom(");
        int end = src.indexOf("public void endGroupMembershipsOnEnrollmentDeactivated(");
        String body = src.substring(start, end);
        assertTrue(body.contains("notifyAdded"));
        assertTrue(body.contains("membersByGroup"));
        assertTrue(body.contains("new ArrayList<>(members)"));
    }

    private int insertGroupSet(int courseId) {
        jdbcTemplate.update("""
                INSERT INTO group_set (course_id, name, default_capacity, locked)
                VALUES (?, 'Set', 10, 0)
                """, courseId);
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM group_set", Integer.class);
        return id == null ? -1 : id;
    }

    private int insertGroup(int courseId, int groupSetId, String name) {
        jdbcTemplate.update("""
                INSERT INTO course_group (group_set_id, course_id, name)
                VALUES (?, ?, ?)
                """, groupSetId, courseId, name);
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM course_group WHERE name = ?", Integer.class, name);
        return id == null ? -1 : id;
    }

    private List<Integer> recipientsOf(String eventKey) {
        return jdbcTemplate.queryForList("""
                SELECT r.recipient_user_id
                FROM notification_event_recipient r
                JOIN notification_event_outbox o ON o.id = r.outbox_id
                WHERE o.event_key = ?
                ORDER BY r.recipient_user_id
                """, Integer.class, eventKey);
    }

    private void assertDisjoint(String keyA, String keyB) {
        Set<Integer> a = new HashSet<>(recipientsOf(keyA));
        Set<Integer> b = new HashSet<>(recipientsOf(keyB));
        a.retainAll(b);
        assertTrue(a.isEmpty(), "audience variants must be disjoint: " + a);
    }
}
