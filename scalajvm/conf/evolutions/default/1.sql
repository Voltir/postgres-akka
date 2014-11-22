# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "notify_akka_user_clocks" ("user_id" BIGINT NOT NULL PRIMARY KEY,"self" BIGINT NOT NULL,"related" BIGINT NOT NULL,"gamez" BIGINT NOT NULL);
create table "notify_akka_users" ("user_id" BIGSERIAL NOT NULL PRIMARY KEY,"gamertag" VARCHAR(254) NOT NULL,"some_foo" VARCHAR(254) NOT NULL);

# --- !Downs

drop table "notify_akka_users";
drop table "notify_akka_user_clocks";

