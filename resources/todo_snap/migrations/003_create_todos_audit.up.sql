CREATE TYPE audit_op AS ENUM (
  'insert',
  'update',
  'delete'
);

-- I read a number of things about audit tables while working on this and the burndown query,
-- including the postgres docs, and an implementation I maintain at work.
-- These were the articles I took the most from:
-- https://www.enterprisedb.com/postgres-tutorials/working-postgres-audit-triggers
-- https://fle.github.io/detect-value-changes-between-successive-lines-with-postgresql.html

CREATE TABLE todos_audit (
  op audit_op NOT NULL,

  id uuid NOT NULL,
  email text NOT NULL,
  title text NOT NULL,
  complete boolean NOT NULL,
  deleted boolean NOT NULL,

  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION audit_info() RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        INSERT INTO todos_audit SELECT 'delete', OLD.*;
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO todos_audit SELECT 'update', NEW.*;
    ELSIF (TG_OP = 'INSERT') THEN
        INSERT INTO todos_audit SELECT 'insert', NEW.*;
    END IF;

    RETURN null;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER todos_audit_trigger AFTER INSERT OR UPDATE OR DELETE ON todos
FOR EACH ROW EXECUTE PROCEDURE audit_info();

CREATE INDEX ON todos_audit (deleted, email);
