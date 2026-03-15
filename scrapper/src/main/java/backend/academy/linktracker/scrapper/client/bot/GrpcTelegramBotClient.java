package backend.academy.linktracker.scrapper.client.bot;

import backend.academy.linktracker.grpc.bot.BotUpdateServiceGrpc;
import backend.academy.linktracker.grpc.bot.LinkUpdateRequest;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.communication", name = "mode", havingValue = "grpc")
public class GrpcTelegramBotClient implements TelegramBotRestClient {

    private final BotUpdateServiceGrpc.BotUpdateServiceBlockingStub botStub;

    @Override
    public void sendUpdate(LinkUpdate linkUpdate) {
        try {
            botStub.sendUpdate(LinkUpdateRequest.newBuilder()
                    .setId(linkUpdate.id())
                    .setDescription(linkUpdate.description())
                    .setUrl(linkUpdate.url())
                    .addAllTgChatIds(linkUpdate.tgChatIds())
                    .build());
            log.atInfo()
                    .setMessage("Успешный запрос к сервису бота")
                    .addKeyValue("event.type", "telegram_bot_request")
                    .addKeyValue("event.status", "success")
                    .log();
        } catch (StatusRuntimeException e) {
            throw handleException(e);
        }
    }

    private BotClientException handleException(StatusRuntimeException e) {
        log.atError()
                .setMessage("Ошибка gRPC вызова")
                .addKeyValue("error.status", e.getStatus().getCode())
                .addKeyValue("error.description", e.getStatus().getDescription())
                .log();
        return new BotClientException(
                e.getStatus().getDescription() != null ? e.getStatus().getDescription() : "gRPC Error");
    }
}
