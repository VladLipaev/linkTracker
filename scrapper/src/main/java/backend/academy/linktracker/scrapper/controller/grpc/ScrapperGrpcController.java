package backend.academy.linktracker.scrapper.controller.grpc;

import backend.academy.linktracker.grpc.scrapper.AddLinkRequest;
import backend.academy.linktracker.grpc.scrapper.ChatRequest;
import backend.academy.linktracker.grpc.scrapper.GetLinksRequest;
import backend.academy.linktracker.grpc.scrapper.LinkResponse;
import backend.academy.linktracker.grpc.scrapper.ListLinksResponse;
import backend.academy.linktracker.grpc.scrapper.RemoveLinkRequest;
import backend.academy.linktracker.grpc.scrapper.ScrapperServiceGrpc;
import backend.academy.linktracker.scrapper.service.CacheLinksUtil;
import backend.academy.linktracker.scrapper.service.ScrapperLinksService;
import backend.academy.linktracker.scrapper.service.TgChatService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.communication.controller", name = "mode", havingValue = "grpc")
public class ScrapperGrpcController extends ScrapperServiceGrpc.ScrapperServiceImplBase {

    private final ScrapperLinksService linksService;
    private final TgChatService tgChatService;
    private final CacheLinksUtil cacheLinksUtil;

    @Override
    public void registerChat(ChatRequest request, StreamObserver<Empty> responseObserver) {
        tgChatService.addTgChat(request.getChatId());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteChat(ChatRequest request, StreamObserver<Empty> responseObserver) {
        cacheLinksUtil.invalidateChatCache(request.getChatId());
        tgChatService.deleteTgChat(request.getChatId());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void addLink(AddLinkRequest request, StreamObserver<LinkResponse> responseObserver) {
        var dtoRequest =
                new backend.academy.linktracker.scrapper.dto.AddLinkRequest(request.getLink(), request.getTagsList());
        var result = linksService.addLink(request.getChatId(), dtoRequest);

        LinkResponse response = LinkResponse.newBuilder()
                .setId(result.id())
                .setUrl(result.url())
                .addAllTags(result.tags())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getLinks(GetLinksRequest request, StreamObserver<ListLinksResponse> responseObserver) {
        String tag = request.hasTag() ? request.getTag() : null;
        var result = linksService.getLinks(request.getChatId(), tag);

        ListLinksResponse.Builder responseBuilder =
                ListLinksResponse.newBuilder().setSize(result.size());

        result.links()
                .forEach(link -> responseBuilder.addLinks(LinkResponse.newBuilder()
                        .setId(link.id())
                        .setUrl(link.url())
                        .addAllTags(link.tags())
                        .build()));

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void removeLink(RemoveLinkRequest request, StreamObserver<LinkResponse> responseObserver) {
        var result = this.linksService.removeLink(
                request.getChatId(), new backend.academy.linktracker.scrapper.dto.RemoveLinkRequest(request.getLink()));

        LinkResponse response = LinkResponse.newBuilder()
                .setId(result.id())
                .setUrl(result.url())
                .addAllTags(result.tags())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
