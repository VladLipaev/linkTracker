--liquibase formatted sql

-- changeset Vlad:add-checked_at
ALTER TABLE links ADD COLUMN checked_at TIMESTAMP WITH TIME ZONE;
