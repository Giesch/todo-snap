-- This query does two subquery scans, which will be slow.
-- To improve this, we could do the work on write, in the audit trigger.
-- There may also be a way to handle it with a materialized view.
-- I'd guess that datomic makes this easier, which is probably the point.

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
                -- NOTE this relies on the where cause below,
                -- which only includes completeness state changes and soft deletions
                WHEN 'update' THEN (CASE (events.complete, events.deleted)
                                        -- undeleted, complete -> incomplete
                                        WHEN (FALSE, FALSE) THEN 1
                                        -- undeleted, incomplete -> complete
                                        WHEN (TRUE, FALSE) THEN -1
                                        -- incomplete, newly deleted
                                        WHEN (FALSE, TRUE) THEN -1
                                        -- complete, newly deleted
                                        WHEN (TRUE, TRUE) THEN 0
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
