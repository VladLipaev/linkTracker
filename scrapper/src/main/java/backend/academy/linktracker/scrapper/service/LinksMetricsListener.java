package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.config.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.service.mapper.LinkAddedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LinksMetricsListener {

    private final ScrapperMetrics scrapperMetrics;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void LinkAddedEventHandler(LinkAddedEvent event){
        scrapperMetrics.incrementLinks(event.domain());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void LinkAddedEventHandler(LinkRemovedEvent event){
        scrapperMetrics.decrementLinks(event.domain());
    }

}
