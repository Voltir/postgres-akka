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
DECLARE
  debug_args text[];;
BEGIN
  SELECT array_append(args,key_val('debug_kind',kind)) INTO debug_args;;
  PERFORM pg_notify('akka_debug', payload)
    FROM
      akka_payload('akka_debug', debug_args) AS payload;;
  RETURN 1;;
END
$$ LANGUAGE plpgsql;

--Akka Tell self if live
CREATE OR REPLACE FUNCTION akka_tell_live_self(self_uid bigint, kind text, args text[])
  RETURNS INT AS $$
BEGIN
  PERFORM pg_notify(live.channel, payload)
  FROM akka_payload(kind, args) AS payload, akka_live_users AS live
  WHERE live.user_id = self_uid;;
  RETURN 1;;
END
$$ LANGUAGE plpgsql;

--Akka Tell Social (self and friends who are live)
--Todo

-- Akka 'Live' Table
CREATE UNLOGGED TABLE akka_live_users ("channel" VARCHAR(64), "user_id" BIGINT NOT NULL);

-- Cache Consistency For USER Table
CREATE OR REPLACE FUNCTION user_modified_trigger()
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
    key_val('user_id',NEW.user_id::text),
    key_val('gamertag',NEW.gamertag),
    key_val('some_foo',NEW.some_foo),
    key_val('lclock',current_clock::text)
  ] INTO mah_args;;
  PERFORM akka_tell_debug('user_modified', mah_args);;
  PERFORM akka_tell_live_self(NEW.user_id,'user_modified',mah_args);;
  RETURN NULL;;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS attached_user_modified_trigger ON notify_akka_users;
CREATE TRIGGER attached_user_modified_trigger
  AFTER INSERT OR UPDATE OR DELETE ON notify_akka_users
  FOR EACH ROW
EXECUTE PROCEDURE user_modified_trigger();

-- Cache Consistency for SOCIAL table
CREATE OR REPLACE FUNCTION social_modified_trigger()
  RETURNS TRIGGER AS $$
DECLARE
  mah_args text[];;
  lclock_A bigint;;
  lclock_B bigint;;
BEGIN
  IF TG_OP in ('INSERT','UPDATE','DELETE') THEN
    WITH updateA AS (
      UPDATE notify_akka_user_clocks
      SET related = related + 1
      WHERE user_id = NEW.uid_a
      RETURNING related
    ) SELECT related INTO lclock_A FROM updateA;;
    WITH updateB AS (
      UPDATE notify_akka_user_clocks
      SET related = related + 1
      WHERE user_id = NEW.uid_b
      RETURNING related
    ) SELECT related INTO lclock_B FROM updateB;;
  END IF;;

  --Check to ensure database is consistent
  IF lclock_A IS NULL THEN
    INSERT INTO notify_akka_user_clocks VALUES (NEW.uid_a,1,1,1);;
    SELECT 1 INTO lclock_A;;
  END IF;;

  IF lclock_B IS NULL THEN
    INSERT INTO notify_akka_user_clocks VALUES (NEW.uid_b,1,1,1);;
    SELECT 1 INTO lclock_B;;
  END IF;;

  --Create Akka Payload Args
  SELECT ARRAY[
    key_val('uid_a',NEW.uid_a::text),
    key_val('uid_b',NEW.uid_b::text),
    key_val('relation',NEW.relation::text),
    key_val('lclock_a',lclock_A::text),
    key_val('lclock_b',lclock_B::text)
  ] INTO mah_args;;
  PERFORM akka_tell_debug('social_modified', mah_args);;
  --PERFORM akka_tell_live_self(NEW.user_id,'user_modified',mah_args);;
  RETURN NULL;;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS attached_social_modified_trigger ON notify_akka_social;
CREATE TRIGGER attached_social_modified_trigger
  AFTER INSERT OR UPDATE OR DELETE ON notify_akka_social
  FOR EACH ROW
EXECUTE PROCEDURE social_modified_trigger();

# --- !Downs

DROP FUNCTION IF EXISTS akka_payload(kind text, args text[]);
DROP FUNCTION IF EXISTS key_val(key text, val text);
DROP FUNCTION IF EXISTS akka_tell_debug(kind text, args text[]);
DROP FUNCTION IF EXISTS  akka_tell_live_self(self_uid bigint, kind text, args text[]);
DROP TABLE IF EXISTS akka_live_users;
DROP TRIGGER IF EXISTS attached_user_modified_trigger ON notify_akka_users;
DROP TRIGGER IF EXISTS attached_social_modified_trigger ON notify_akka_social;
DROP FUNCTION IF EXISTS social_modified_trigger()
DROP FUNCTION IF EXISTS user_modified_trigger()