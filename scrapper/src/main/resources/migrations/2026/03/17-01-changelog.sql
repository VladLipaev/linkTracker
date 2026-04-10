--liquibase formatted sql

--changeset Vlad:fix-1

alter table subscriptions
drop constraint subscriptions_chat_id_fkey,
drop constraint subscriptions_link_id_fkey,

add constraint subscriptions_chat_id_fkey
    foreign key (chat_id)
        references chats(id) on delete cascade,
add constraint subscriptions_link_id_fkey
    foreign key (link_id)
        references links(id) on delete cascade;

--changeset Vlad:index-1

create unique index idx_links_url on links(url);

create index idx_links_updated_at on links(updated_at);
