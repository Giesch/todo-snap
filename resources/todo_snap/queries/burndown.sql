-- This query does two subquery scans, which will be slow.
-- We could do the work on write, in the audit trigger.
-- I'm guessing that datomic also makes this easier, which is probably the point.
--
-- I read a number of things about audit tables while working on this,
-- including an implementation I maintain at work. This was the one I took the most from:
-- https://fle.github.io/detect-value-changes-between-successive-lines-with-postgresql.html

WITH events AS (
    SELECT (CASE audit.op
                WHEN 'update' THEN
                    LAG(audit.complete) OVER (
                        PARTITION BY audit.id ORDER BY audit.updated_at ASC
                    )
                ELSE NULL
            END) AS prev_complete,

            LAG(audit.deleted) OVER (
                PARTITION BY audit.id ORDER BY audit.updated_at ASC
            ) AS prev_deleted,

            audit.complete,
            audit.deleted,
            audit.updated_at,
            audit.op,
            audit.title,
            audit.id
    FROM todos_audit audit
    WHERE audit.email = ?
    ORDER BY audit.id, audit.updated_at ASC
),

changes AS (
    SELECT *,
        (CASE events.op
                WHEN 'insert' THEN 1
                WHEN 'delete' THEN -1
                WHEN 'update' THEN (CASE (events.complete, events.deleted)
                                        WHEN (FALSE, FALSE) THEN 1
                                        ELSE -1
                                    END)
            END) AS change
    FROM events
    WHERE (events.op != 'update')
       OR (events.complete IS DISTINCT FROM events.prev_complete)
       OR (events.deleted IS DISTINCT FROM events.prev_deleted)
    ORDER BY events.updated_at ASC
)

SELECT *,
    SUM(changes.change) OVER
        (ORDER BY changes.updated_at ASC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
    AS burndown_total
FROM changes;
