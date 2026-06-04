-- liquibase formatted sql

-- changeset Vlad:processed_event_table

CREATE TABLE processed_event (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
