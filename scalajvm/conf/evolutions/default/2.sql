# --- !Ups
CREATE OR REPLACE FUNCTION user_modified_sync_clocks()
  RETURNS TRIGGER AS $$
DECLARE
  mah_args text[];;
BEGIN
  IF NEW.gamertag IS NULL THEN
    RAISE EXCEPTION 'gamertag cannot be null';;
  END IF;;

  IF TG_OP = 'INSERT' THEN
    INSERT INTO notify_akka_user_clocks VALUES (NEW.user_id,1,1,1);;
  ELSEIF TG_OP = 'UPDATE' THEN
    UPDATE notify_akka_user_clocks
      SET self = self + 1
      WHERE user_id = NEW.user_id;;
  END IF;;

  --Create Debug Payload Args
  SELECT ARRAY[
    key_val('gamertag',NEW.gamertag),
    key_val('baz','baz')
  ] INTO mah_args;;
  PERFORM pg_notify('akka_debug',result) FROM akka_payload('user_modified', mah_args) AS result;;

  RETURN NULL;;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS user_modified_trigger ON notify_akka_users;
CREATE TRIGGER user_modified_trigger
  AFTER INSERT ON notify_akka_users
  FOR EACH ROW
EXECUTE PROCEDURE user_modified_sync_clocks();

# --- !Downs

DROP TRIGGER user_modified_trigger ON notify_akka_users;
DROP FUNCTION IF EXISTS user_modified_sync_clocks();