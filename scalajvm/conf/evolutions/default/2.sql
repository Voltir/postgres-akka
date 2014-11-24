# --- !Ups

-- Akka Helper Utilities
CREATE OR REPLACE FUNCTION key_val(key text, val text)
  RETURNS text AS $$
BEGIN
  RETURN '["' || key || '","' || val || '"]';;
END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION akka_payload(kind text, args text[])
  RETURNS text AS $$
BEGIN
  RETURN '["' || kind || '",[' || array_to_string(args,',') || ']]';;
END
$$ LANGUAGE plpgsql;

--Akka Tell Debug
CREATE OR REPLACE FUNCTION akka_tell_debug(kind text, args text[])
  RETURNS INT AS $$
BEGIN
  PERFORM pg_notify('akka_debug', payload)
  FROM akka_payload(kind, args) AS payload;;
  RETURN 1;;
END
$$ LANGUAGE plpgsql;

--Akka Tell self if live
CREATE OR REPLACE FUNCTION akka_tell_live_self(self_uid bigint, kind text, args text[])
  RETURNS INT AS $$
BEGIN
  PERFORM pg_notify('_c'||self_uid, payload)
  FROM akka_payload(kind, args) AS payload, akka_live_users AS live
  WHERE live.user_id = self_uid;;
  RETURN 1;;
END
$$ LANGUAGE plpgsql;

--Akka Tell Social (self and friends who are live)
--Todo

-- Akka 'Live' Table
CREATE UNLOGGED TABLE akka_live_users ("channel" VARCHAR(64), "user_id" BIGINT NOT NULL);

-- Cache Consistency For User Table
CREATE OR REPLACE FUNCTION user_modified_sync_clocks()
  RETURNS TRIGGER AS $$
DECLARE
  mah_args text[];;
  current_clock bigint;;
BEGIN
  IF TG_OP = 'INSERT' THEN
    INSERT INTO notify_akka_user_clocks VALUES (NEW.user_id,1,1,1);;
    SELECT 1 INTO current_clock;;
  ELSEIF TG_OP = 'UPDATE' THEN
    WITH updated AS (
      UPDATE notify_akka_user_clocks
      SET self = self + 1
      WHERE user_id = NEW.user_id
      RETURNING self
    ) SELECT self INTO current_clock FROM updated;;
  ELSEIF TG_OP = 'DELETE' THEN
    DELETE FROM notify_akka_user_clocks WHERE user_id = OLD.user_id;;
    SELECT 0 INTO current_clock;;
  END IF;;

  --Create Debug Payload Args
  SELECT ARRAY[
    key_val('gamertag',NEW.gamertag),
    key_val('lclock',current_clock::text),
    key_val('baz','baz')
  ] INTO mah_args;;
  PERFORM akka_tell_debug('user_modified', mah_args);;
  PERFORM akka_tell_live_self(NEW.user_id,'self_modified',mah_args);;
  RETURN NULL;;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS user_modified_trigger ON notify_akka_users;
CREATE TRIGGER user_modified_trigger
  AFTER INSERT OR UPDATE OR DELETE ON notify_akka_users
  FOR EACH ROW
EXECUTE PROCEDURE user_modified_sync_clocks();

# --- !Downs

DROP FUNCTION akka_payload(kind text, args text[]);
DROP FUNCTION key_val(key text, val text);
DROP FUNCTION akka_tell_debug(kind text, args text[]);
DROP FUNCTION akka_tell_live_self(self_uid bigint, kind text, args text[]);
DROP TABLE akka_live_users;
DROP TRIGGER user_modified_trigger ON notify_akka_users;
DROP FUNCTION IF EXISTS user_modified_sync_clocks();