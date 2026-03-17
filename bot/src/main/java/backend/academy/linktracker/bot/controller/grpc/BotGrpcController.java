package backend.academy.linktracker.bot.controller.grpc;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.TelegramUpdateService;
import backend.academy.linktracker.grpc.bot.BotUpdateServiceGrpc;
import backend.academy.linktracker.grpc.bot.LinkUpdateRequest;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.communication", name = "mode", havingValue = "grpc")
public class BotGrpcController extends BotUpdateServiceGrpc.BotUpdateServiceImplBase {

    private final TelegramUpdateService telegramUpdateService;

    @Override
    public void sendUpdate(LinkUpdateRequest request, StreamObserver<Empty> responseObserver) {
        LinkUpdate linkUpdate =
                new LinkUpdate(
                    request.getId(),
                    request.getUrl(),
                    request.getDescription(),
                    request.getTgChatIdsList());
        telegramUpdateService.postUpdate(linkUpdate);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
