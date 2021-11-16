CREATE TYPE audit_op AS ENUM (
  'insert',
  'update',
  'delete'
);

-- audit table implementation taken from here:
-- https://www.enterprisedb.com/postgres-tutorials/working-postgres-audit-triggers

CREATE TABLE todos_audit (
  op audit_op NOT NULL,
  stamp timestamptz NOT NULL,

  id uuid NOT NULL,
  title text NOT NULL,
  complete boolean NOT NULL,
  deleted boolean NOT NULL
);

CREATE OR REPLACE FUNCTION foo_audit_info() RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        INSERT INTO todos_audit SELECT 'delete', now(), OLD.*;
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO todos_audit SELECT 'update', now(), NEW.*;
    ELSIF (TG_OP = 'INSERT') THEN
        INSERT INTO todos_audit SELECT 'insert', now(), NEW.*;
    END IF;

    RETURN null;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER todos_audit_trigger AFTER INSERT OR UPDATE OR DELETE ON todos
FOR EACH ROW EXECUTE PROCEDURE foo_audit_info();
