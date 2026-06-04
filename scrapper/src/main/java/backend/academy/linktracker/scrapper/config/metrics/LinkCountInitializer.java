package backend.academy.linktracker.scrapper.config.metrics;

// или нужный сервис
import backend.academy.linktracker.scrapper.repository.orm.JpaLinksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LinkCountInitializer implements ApplicationRunner {
    private final ScrapperMetrics metrics;
    private final JpaLinksRepository linksRepository;

    @Override
    public void run(ApplicationArguments args) {
        long githubCount = linksRepository.countByUrlContaining("github.com");
        long stackoverflowCount = linksRepository.countByUrlContaining("stackoverflow.com");

        metrics.initLinkCount("github.com", (int) githubCount);
        metrics.initLinkCount("stackoverflow.com", (int) stackoverflowCount);
    }
}
