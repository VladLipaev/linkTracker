-- liquibase formatted sql

-- changeset Vlad:outbox_table_fix_2

drop table if exists outbox_link_update;
drop index if exists idx_created_at_where_can_send;
drop index if exists idx_created_at_where_cant_send;


create table outbox_link_update
(
    id            uuid primary key,
    aggregatetype VARCHAR(255) NOT NULL,
    aggregateid VARCHAR(255) NOT NULL,
    eventtype VARCHAR(255) NOT NULL,
    payload       JSON         NOT NULL,
    partition_key varchar(255) not null,
    status        varchar(25)              default 'new', --sent, error
    retry_count   int                      default 0,
    trace_id varchar(32) not null,
    span_id varchar(16) not null,
    created_at    timestamp with time zone default now(),
    processed_at  timestamp with time zone
);
create index idx_created_at_where_can_send
    on outbox_link_update (created_at)
    where status in ('new', 'error');
create index idx_created_at_where_cant_send on outbox_link_update (status, processed_at) where status = 'sent' or (status = 'error' and retry_count >= 5);
create index idx_outbox_link_update_status_created_at on outbox_link_update(status, created_at);
