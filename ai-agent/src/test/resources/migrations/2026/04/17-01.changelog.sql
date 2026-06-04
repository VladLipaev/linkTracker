-- liquibase formatted sql

-- changeset Vlad:outbox_table_fix

drop table if exists outbox_link_update;

create table outbox_link_update(
                                   id uuid primary key,
                                   payload text not null,
                                   partition_key varchar(255) not null,
                                   status varchar(25) default 'new', --sent, error
                                   retry_count int default 0,
                                   created_at timestamp with time zone default now(),
                                   processed_at timestamp with time zone

);
create index idx_created_at_where_can_send
    on outbox_link_update(created_at)
    where status in ('new', 'error');
create index idx_created_at_where_cant_send on outbox_link_update(status, processed_at) where status = 'sent' or (status = 'error' and retry_count >= 5);
