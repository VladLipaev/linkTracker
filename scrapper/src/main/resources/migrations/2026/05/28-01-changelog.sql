-- liquibase formatted sql

-- changeset Vlad:raw_processed_event_table

CREATE TABLE raw_processed_event (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- changeset Vlad:outbox_processed_link_update

create table outbox_processed_link_update(
                                   id uuid primary key,
                                   payload text not null,
                                   partition_key varchar(255) not null,
                                   status varchar(25) default 'new', --sent, error
                                   retry_count int default 0,
                                   created_at timestamp with time zone default now(),
                                   processed_at timestamp with time zone

);
create index idx_created_at_where_can_send_processed
    on outbox_processed_link_update(created_at)
    where status in ('new', 'error');
create index idx_created_at_where_cant_send_processed on outbox_processed_link_update(status, processed_at) where status = 'sent' or (status = 'error' and retry_count >= 5);
