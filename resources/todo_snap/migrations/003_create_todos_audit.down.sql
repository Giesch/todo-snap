DROP TRIGGER IF EXISTS todos_audit_trigger ON todos;

DROP TABLE IF EXISTS todos_audit;

DROP TYPE IF EXISTS audit_op;
