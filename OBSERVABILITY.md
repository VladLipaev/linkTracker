# Observability

## Подключенные метрики

### Scrapper (порт 8081, эндпоинт `/metrics`)

| Метрика | Тип | Лейблы | Описание |
|---------|-----|--------|----------|
| `links_on_track_total` | Gauge | `tracked_source` | Количество отслеживаемых ссылок по доменам |
| `request_duration_ms` | Histogram | `scope`, `scope_type` | Длительность операций (БД, внешние API, Kafka) |
| `api_requests_total` | Counter | `source` | Входящие API-запросы |

Бакеты гистограммы: 10, 50, 100, 200, 500, 1000, 2000, 5000 мс.

### Bot (порт 8011, эндпоинт `/metrics`)

| Метрика | Тип | Лейблы | Описание |
|---------|-----|--------|----------|
| `command_requests_total` | Counter | `command` | Обработанные команды (`/start`, `/track` и др.) |
| `command_duration_ms` | Histogram | `scope`, `scope_type` | Длительность обработки команд |
| `sent_notification_total` | Counter | — | Отправленные нотификации |

Бакеты гистограммы: 10, 50, 100, 200, 500, 1000, 2000, 5000 мс.

## PromQL запросы

### RED метрики
- **Rate**: `rate(http_server_requests_seconds_count{application=~"$app"}[1m])`
- **Errors**: `rate(http_server_requests_seconds_count{application=~"$app", status=~"5.."}[1m])`
- **Latency p99**: `histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket{application=~"$app"}[1m])) by (le)) * 1000`

### Память
- **Heap Used**: `jvm_memory_used_bytes{application=~"$app", area="heap"} / 1048576`
- **Non-Heap Used**: `jvm_memory_used_bytes{application=~"$app", area="nonheap"} / 1048576`

### Бизнес-метрики
- **Сообщения пользователей**: `sum(rate(command_requests_total{application="bot"}[1m]))`
- **Активные ссылки**: `links_on_track_total`
- **Длительность scrape (p50/p95/p99)**: `histogram_quantile(0.50/0.95/0.99, sum(rate(request_duration_ms_bucket{scope="external_source", scope_type="$source"}[5m])) by (le))`
- **Длительность команд (p50/p95/p99)**: `histogram_quantile(0.50/0.95/0.99, sum(rate(command_duration_ms_bucket{scope="scrapper_sync_api", scope_type="$command"}[5m])) by (le))`
- **Запросы к боту**: `sum(rate(command_requests_total{application="bot"}[1m]))`
- **Нотификации**: `rate(sent_notification_total{application="bot"}[1m])`
