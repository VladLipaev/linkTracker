package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.api.SubscriptionsApi;
import backend.academy.linktracker.scrapper.dto.PageSubscriptionsResponse;
import backend.academy.linktracker.scrapper.service.SubscriptionService;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubscriptionsRestController implements SubscriptionsApi {

    private final SubscriptionService subscriptionService;

    @Override
    public ResponseEntity<PageSubscriptionsResponse> getSubscriptions(Integer page,
                                                                      Integer size,
                                                                      String sort,
                                                                      String tag,
                                                                      String urlContains,
                                                                      OffsetDateTime lastCheckBefore,
                                                                      OffsetDateTime lastCheckAfter,
                                                                      Long chatId) {
        return ResponseEntity.ok(subscriptionService.getPageSubscriptionsByFilter(page, size, sort, tag, urlContains, lastCheckBefore, lastCheckAfter, chatId));
    }
}
