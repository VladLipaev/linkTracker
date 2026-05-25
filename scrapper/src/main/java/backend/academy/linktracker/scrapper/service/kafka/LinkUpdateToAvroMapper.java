package backend.academy.linktracker.scrapper.service.kafka;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.avro.LinkUpdateAvro;
import backend.academy.linktracker.scrapper.dto.avro.RawLinkUpdateAvro;
import backend.academy.linktracker.scrapper.handler.GitHubLinkHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LinkUpdateToAvroMapper {

    private final GitHubLinkHandler gitHubLinkHandler;

    public LinkUpdateAvro linkUpdateAvro(LinkUpdate payload) {
        return LinkUpdateAvro.newBuilder()
                .setId(payload.id())
                .setDescription(payload.description())
                .setUrl(payload.url())
                .setTgChatIds(payload.tgChatIds())
                .build();
    }

    @SneakyThrows
    public RawLinkUpdateAvro rawLinkUpdateAvro(LinkUpdate linkUpdate) {
        String description = linkUpdate.description();
        String url = linkUpdate.url();

        if (description == null) {
            description = "";
        }

        if (gitHubLinkHandler.supports(url)) {
            Pattern pattern1 = Pattern.compile("Обновления в репозитории: \\*\\s*([^/]+)");
            Matcher matcher1 = pattern1.matcher(description);

            Pattern pattern2 = Pattern.compile("Автор:\\s*([^\n]+)");
            Matcher matcher2 = pattern2.matcher(description);

            String repoAuthor = "";
            String author = "";

            if (matcher1.find()) {
                repoAuthor = matcher1.group(1).trim();
            }
            if (matcher2.find()) {
                author = matcher2.group(1).trim();
            }

            String rawLinkUpdateAuthor = "self";
            if (!repoAuthor.isEmpty() && !author.isEmpty() && !repoAuthor.equals(author)) {
                rawLinkUpdateAuthor = author;
            }

            return RawLinkUpdateAvro.newBuilder()
                    .setId(linkUpdate.id())
                    .setDescription(description)
                    .setAuthor(rawLinkUpdateAuthor)
                    .setTgChatIds(linkUpdate.tgChatIds())
                    .build();

        } else {
            Pattern pattern = Pattern.compile("\\*\\* от\\s*([^\n]+)");
            Matcher matcher = pattern.matcher(description);

            String rawLinkUpdateAuthor = "self";
            if (matcher.find()) {
                rawLinkUpdateAuthor = matcher.group(1).trim();
            }

            return RawLinkUpdateAvro.newBuilder()
                    .setId(linkUpdate.id())
                    .setDescription(description)
                    .setAuthor(rawLinkUpdateAuthor)
                    .setTgChatIds(linkUpdate.tgChatIds())
                    .build();
        }
    }
}
