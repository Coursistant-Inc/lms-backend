SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE nw_review;
TRUNCATE TABLE nw_submission;
TRUNCATE TABLE nw_assignment;
TRUNCATE TABLE nw_course_unit;
TRUNCATE TABLE rel_user_course;
TRUNCATE TABLE nw_course;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO nw_course (id,
                       created_at,
                       updated_at,
                       course_code,
                       name,
                       description,
                       school,
                       semester,
                       teacher_id)
WITH RankedCourses AS (SELECT c.*,
                              ROW_NUMBER() OVER (PARTITION BY c.course_code ORDER BY c.id) as rn
                       FROM Course c
                       WHERE c.teacher_id IS NOT NULL)
SELECT rc.id,
       NOW()                            as created_at,
       NOW()                            as updated_at,
       SUBSTRING(rc.course_code, 1, 16) as course_code,
       SUBSTRING(rc.name, 1, 127)       as name,
       COALESCE(rc.title, '')           as description,
       SUBSTRING(rc.school, 1, 127)     as school,
       rc.semester,
       rc.teacher_id
FROM RankedCourses rc
WHERE rc.rn = 1
UNION ALL
SELECT c.id,
       NOW()                           as created_at,
       NOW()                           as updated_at,
       SUBSTRING(c.course_code, 1, 16) as course_code,
       SUBSTRING(c.name, 1, 127)       as name,
       COALESCE(c.title, '')           as description,
       SUBSTRING(c.school, 1, 127)     as school,
       c.semester,
       299                             as teacher_id
FROM Course c
WHERE c.teacher_id IS NULL
  AND NOT EXISTS (SELECT 1
                  FROM Course c2
                  WHERE c2.course_code = c.course_code
                    AND c2.teacher_id IS NOT NULL);

INSERT INTO rel_user_course (id,
                             user_id,
                             course_id)
SELECT t.id,
       t.user_id,
       t.course_id
FROM Teach t
WHERE t.user_id IS NOT NULL
  AND t.course_id IS NOT NULL
  AND EXISTS (SELECT 1
              FROM nw_course nc
              WHERE nc.id = t.course_id)
  AND EXISTS (SELECT 1
              FROM User u
              WHERE u.id = t.user_id);

INSERT INTO nw_course_unit (id,
                            created_at,
                            updated_at,
                            sort_order,
                            title,
                            description,
                            course_id)
SELECT f.id,
       NOW()                                               as created_at,
       NOW()                                               as updated_at,
       GREATEST(COALESCE(f.order_index, 0), 0)             as sort_order,
       COALESCE(SUBSTRING(f.name, 1, 63), 'Untitled unit') as title,
       ''                                                  as description,
       f.course_id
FROM Folder f
WHERE EXISTS (SELECT 1
              FROM nw_course nc
              WHERE nc.id = f.course_id);

UPDATE nw_course_unit ncu
    JOIN Folder f ON f.id = ncu.id
    LEFT JOIN (SELECT folder_id,
    GROUP_CONCAT(
    COALESCE(content, '')
    SEPARATOR '\n'
    ) as all_content
    FROM FolderItem
    WHERE folder_id IS NOT NULL
    GROUP BY folder_id) fi ON fi.folder_id = f.id
    SET ncu.description = CONCAT_WS(
        '\n',
        COALESCE(f.description, ''),
        COALESCE(fi.all_content, '')
        )
WHERE ncu.description = '';

UPDATE nw_course_unit
SET description = 'No description...'
WHERE description = '';

INSERT INTO nw_assignment (id,
                           created_at,
                           updated_at,
                           title,
                           description,
                           type,
                           due_time,
                           settings,
                           course_unit_id)
SELECT a.id,
       a.due                                                             as created_at,
       COALESCE(a.updated_at, a.due)                                     as updated_at,
       COALESCE(SUBSTRING(a.title, 1, 63), 'Untitled assignment')        as title,
       a.description                                                     as description,
       COALESCE(a.criteria, 'homework')                                  as type,
       COALESCE(
               TIMESTAMP(a.due),
               TIMESTAMP(DATE_ADD(NOW(), INTERVAL 7 DAY))
       )                                                                 as due_time,
       '{"allowLateSubmission": false, "allowedResubmissionCount": 100}' as settings,
       a.course_content_id                                               as course_unit_id
FROM Assignment a
WHERE a.course_content_id IS NOT NULL
  AND EXISTS (SELECT 1
              FROM nw_course_unit ncu
              WHERE ncu.id = a.course_content_id);

INSERT INTO nw_submission (
    id,
    created_at,
    updated_at,
    submission_count,
    submission_content,
    student_id,
    assignment_id
)
SELECT
    asub.id,
    COALESCE(asub.date, NOW()) AS created_at,
    COALESCE(asub.date, NOW()) AS updated_at,
    1 AS submission_count,
    COALESCE(asub.student_comment, '') AS submission_content,
    asub.student_id,
    asub.assignment_id
FROM AssignmentSubmission asub
WHERE asub.assignment_id IN (SELECT id FROM nw_assignment)
  AND asub.student_id IN (SELECT id FROM User)
  AND asub.id IN (
    SELECT MAX(asub2.id)
    FROM AssignmentSubmission asub2
    WHERE asub2.assignment_id = asub.assignment_id
      AND asub2.student_id = asub.student_id
    GROUP BY asub2.assignment_id, asub2.student_id
);

INSERT INTO nw_review (
    id,
    created_at,
    updated_at,
    grade,
    teacher_comment,
    submission_id
)
SELECT
    ROW_NUMBER() OVER (ORDER BY asub.id) +
    (SELECT COALESCE(MAX(id), 0) FROM nw_review) AS id,
    COALESCE(asub.date, NOW()) AS created_at,
    COALESCE(asub.date, NOW()) AS updated_at,
    CASE
        WHEN asub.grade IS NOT NULL THEN ROUND(asub.grade)
        ELSE 0
        END AS grade,
    COALESCE(asub.comment, '') AS teacher_comment,
    asub.id AS submission_id
FROM AssignmentSubmission asub
         INNER JOIN nw_submission ns ON ns.id = asub.id
WHERE asub.grade IS NOT NULL
   OR (asub.comment IS NOT NULL AND asub.comment != '');


DELETE
FROM nw_assignment
WHERE course_unit_id NOT IN (SELECT id FROM nw_course_unit);

DELETE
FROM nw_course_unit
WHERE course_id NOT IN (SELECT id FROM nw_course);

DELETE
FROM rel_user_course
WHERE user_id NOT IN (SELECT id FROM User);

DELETE
FROM rel_user_course
WHERE course_id NOT IN (SELECT id FROM nw_course);

DELETE FROM nw_submission
WHERE assignment_id NOT IN (SELECT id FROM nw_assignment)
   OR student_id NOT IN (SELECT id FROM User);

DELETE FROM nw_review
WHERE submission_id NOT IN (SELECT id FROM nw_submission);