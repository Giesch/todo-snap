-- This query is a candidate for turning into a view,
-- or for using datomic features (which I'd guess is the point).
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
            audit.complete,
            audit.updated_at,
            audit.op,
            audit.title,
            audit.id
    FROM todos_audit audit
    WHERE NOT audit.deleted AND audit.email = ?
    ORDER BY audit.id, audit.updated_at ASC
),

changes AS (
    SELECT *,
        (CASE events.op
                WHEN 'insert' THEN 1
                WHEN 'delete' THEN -1
                WHEN 'update' THEN (CASE events.complete
                                        WHEN TRUE THEN -1
                                        ELSE 1
                                    END)
            END) AS change
    FROM events
    WHERE (events.op != 'update')
       OR (events.complete IS DISTINCT FROM events.prev_complete)
    ORDER BY events.updated_at ASC
)

SELECT *,
    SUM(changes.change) OVER
        (ORDER BY changes.updated_at ASC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
    AS burndown_total
FROM changes;
