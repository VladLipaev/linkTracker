INSERT INTO chats (id)
SELECT generate_series(1, 1000);

INSERT INTO links (url, updated_at)
SELECT 'https://github.com/VladLipaev/' || i, now()
FROM generate_series(1, 100000) AS i;

INSERT INTO subscriptions (chat_id, link_id)
SELECT
    ((i - 1) / 100) + 1 AS chat_id,
    i AS link_id
FROM generate_series(1, 100000) AS i;
