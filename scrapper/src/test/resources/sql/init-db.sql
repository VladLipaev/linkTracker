-- liquibase formatted sql

--changeset Vlad:init-0
create table chats(
                      id bigint primary key
);

create table links(

                      id bigserial primary key,
                      url varchar(2048) not null unique,
                      updated_at timestamptz default now()
);

create table subscriptions(
                              chat_id bigint not null references chats(id) on delete cascade,
                              link_id bigint not null references links(id) on delete cascade,
                              primary key (chat_id, link_id)
);

create table subscriptions_tags(
                                   chat_id bigint not null,
                                   link_id bigint not null,
                                   tag varchar(255) not null,
                                   primary key (chat_id, link_id, tag),
                                   constraint fk_subscription foreign key
                                       (chat_id, link_id) references subscriptions(chat_id, link_id) on delete cascade
);

create index idx_links_updated_at on links(updated_at);
