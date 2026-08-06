-- course_part1_backfill.sql (re-entrant)
-- Maps creator_id → USER actor. Does not invent SYSTEM_ADMIN/TENANT_ADMIN creators.

UPDATE course c
INNER JOIN `user` u ON u.id = c.creator_id AND u.role = 'USER'
SET c.creator_actor_type = 'USER',
    c.creator_actor_id = c.creator_id,
    c.creator_role = COALESCE(u.role, 'USER')
WHERE c.creator_actor_type IS NULL
   OR c.creator_actor_id IS NULL;
