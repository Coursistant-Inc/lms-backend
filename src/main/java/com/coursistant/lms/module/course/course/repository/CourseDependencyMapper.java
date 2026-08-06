package com.coursistant.lms.module.course.course.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Counts non-empty course dependents for Empty Course Delete.
 * Tables that may not exist in every env are checked via information_schema-safe counts
 * only for known greenfield tables in this repo.
 */
@Mapper
public interface CourseDependencyMapper {

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM enrollment
                 WHERE course_id = #{courseId}
                   AND NOT (course_role = 'Instructor' AND active = 1)) AS extraEnrollments,
              (SELECT COUNT(*) FROM course_week WHERE course_id = #{courseId}) AS weeks,
              (SELECT COUNT(*) FROM course_material cm
                 INNER JOIN course_week cw ON cw.id = cm.week_id
                WHERE cw.course_id = #{courseId}) AS materials,
              (SELECT COUNT(*) FROM course_syllabus WHERE course_id = #{courseId}) AS syllabi,
              (SELECT COUNT(*) FROM course_session WHERE course_id = #{courseId}) AS sessions,
              (SELECT COUNT(*) FROM course_event WHERE course_id = #{courseId}) AS events,
              (SELECT COUNT(*) FROM assignment WHERE course_id = #{courseId}) AS assignments,
              (SELECT COUNT(*) FROM quiz WHERE course_id = #{courseId}) AS quizzes,
              (SELECT COUNT(*) FROM group_set WHERE course_id = #{courseId}) AS groupSets,
              (SELECT COUNT(*) FROM course_announcement WHERE course_id = #{courseId}) AS announcements
            """)
    CourseDependencyCounts countDependencies(@Param("courseId") Integer courseId);

    class CourseDependencyCounts {
        private long extraEnrollments;
        private long weeks;
        private long materials;
        private long syllabi;
        private long sessions;
        private long events;
        private long assignments;
        private long quizzes;
        private long groupSets;
        private long announcements;

        public boolean hasAny() {
            return extraEnrollments > 0
                    || weeks > 0
                    || materials > 0
                    || syllabi > 0
                    || sessions > 0
                    || events > 0
                    || assignments > 0
                    || quizzes > 0
                    || groupSets > 0
                    || announcements > 0;
        }

        public long getExtraEnrollments() {
            return extraEnrollments;
        }

        public void setExtraEnrollments(long extraEnrollments) {
            this.extraEnrollments = extraEnrollments;
        }

        public long getWeeks() {
            return weeks;
        }

        public void setWeeks(long weeks) {
            this.weeks = weeks;
        }

        public long getMaterials() {
            return materials;
        }

        public void setMaterials(long materials) {
            this.materials = materials;
        }

        public long getSyllabi() {
            return syllabi;
        }

        public void setSyllabi(long syllabi) {
            this.syllabi = syllabi;
        }

        public long getSessions() {
            return sessions;
        }

        public void setSessions(long sessions) {
            this.sessions = sessions;
        }

        public long getEvents() {
            return events;
        }

        public void setEvents(long events) {
            this.events = events;
        }

        public long getAssignments() {
            return assignments;
        }

        public void setAssignments(long assignments) {
            this.assignments = assignments;
        }

        public long getQuizzes() {
            return quizzes;
        }

        public void setQuizzes(long quizzes) {
            this.quizzes = quizzes;
        }

        public long getGroupSets() {
            return groupSets;
        }

        public void setGroupSets(long groupSets) {
            this.groupSets = groupSets;
        }

        public long getAnnouncements() {
            return announcements;
        }

        public void setAnnouncements(long announcements) {
            this.announcements = announcements;
        }
    }
}
