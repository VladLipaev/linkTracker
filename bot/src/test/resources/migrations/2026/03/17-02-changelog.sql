--liquibase formatted sql

--changeset Vlad:fix-2

drop index idx_links_url;
