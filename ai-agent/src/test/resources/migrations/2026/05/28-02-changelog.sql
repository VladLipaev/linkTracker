-- liquibase formatted sql

-- changeset Vlad:processed_link_update_table

create table processed_link_update(
    id uuid primary key default gen_random_uuid(),
    original_event_id bigint,
    description text not null,
    tg_chat_id bigint not null,
    priority varchar(10) not null,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed BOOLEAN DEFAULT FALSE,
    retry_count int default 0
);
