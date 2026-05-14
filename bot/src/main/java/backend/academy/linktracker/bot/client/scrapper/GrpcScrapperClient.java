package backend.academy.linktracker.bot.client.scrapper;

import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import backend.academy.linktracker.grpc.scrapper.*;
import io.grpc.StatusRuntimeException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.communication.client", name = "mode", havingValue = "grpc")
public class GrpcScrapperClient implements ScrapperClient {

    private final ScrapperServiceGrpc.ScrapperServiceBlockingStub scrapperStub;

    @Override
    public void registerChat(long chatId) {
        try {
            scrapperStub.registerChat(ChatRequest.newBuilder().setChatId(chatId).build());
        } catch (StatusRuntimeException e) {
            throw handleException(e);
        }
    }

    @Override
    public LinkResponse addLink(long chatId, String link, List<String> tags) {
        try {
            AddLinkRequest request = AddLinkRequest.newBuilder()
                    .setChatId(chatId)
                    .setLink(link)
                    .addAllTags(tags)
                    .build();

            backend.academy.linktracker.grpc.scrapper.LinkResponse response = scrapperStub.addLink(request);

            return new LinkResponse(response.getId(), response.getUrl(), response.getTagsList());
        } catch (StatusRuntimeException e) {
            throw handleException(e);
        }
    }

    @Override
    public ListLinksResponse getLinks(long chatId, String tag) {
        try {
            GetLinksRequest.Builder requestBuilder =
                    GetLinksRequest.newBuilder().setChatId(chatId);
            if (tag != null) {
                requestBuilder.setTag(tag);
            }
            backend.academy.linktracker.grpc.scrapper.ListLinksResponse response =
                    scrapperStub.getLinks(requestBuilder.build());

            List<LinkResponse> links = response.getLinksList().stream()
                    .map(l -> new LinkResponse(l.getId(), l.getUrl(), l.getTagsList()))
                    .toList();

            return new ListLinksResponse(links, response.getSize());
        } catch (StatusRuntimeException e) {
            throw handleException(e);
        }
    }

    @Override
    public LinkResponse removeLink(long chatId, String url) {
        try {
            RemoveLinkRequest requestBuilder = RemoveLinkRequest.newBuilder()
                    .setChatId(chatId)
                    .setLink(url)
                    .build();
            backend.academy.linktracker.grpc.scrapper.LinkResponse response = scrapperStub.removeLink(requestBuilder);

            LinkResponse linkResponse = new LinkResponse(response.getId(), response.getUrl(), response.getTagsList());
            return linkResponse;
        } catch (StatusRuntimeException e) {
            throw handleException(e);
        }
    }

    private ScrapperClientException handleException(StatusRuntimeException e) {
        log.atError()
                .setMessage("Ошибка gRPC вызова")
                .addKeyValue("error.status", e.getStatus().getCode())
                .addKeyValue("error.description", e.getStatus().getDescription())
                .log();
        return new ScrapperClientException(
                e.getStatus().getDescription() != null ? e.getStatus().getDescription() : "gRPC Error", e);
    }
}
