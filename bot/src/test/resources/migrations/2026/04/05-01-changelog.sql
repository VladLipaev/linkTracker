-- liquibase formatted sql

-- changeset Vlad:fix-3

alter table links
alter column updated_at set not null;

-- changeset Vlad:index-2

create index idx_subscriptions_tags_tag on subscriptions_tags(tag);
