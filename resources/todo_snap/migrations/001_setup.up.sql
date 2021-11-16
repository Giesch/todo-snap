CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- taken from diesel.rs' manage_updated_at function
-- https://github.com/diesel-rs/diesel/blob/master/migrations/postgresql/00000000000000_diesel_initial_setup/up.sql

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    IF (
        NEW IS DISTINCT FROM OLD AND
        NEW.updated_at IS NOT DISTINCT FROM OLD.updated_at
    ) THEN
        NEW.updated_at := current_timestamp;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
