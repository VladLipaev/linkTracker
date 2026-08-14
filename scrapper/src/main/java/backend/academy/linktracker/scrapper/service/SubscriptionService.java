package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.PageSubscriptionsResponse;
import java.time.OffsetDateTime;

public interface SubscriptionService {

    PageSubscriptionsResponse getPageSubscriptionsByFilter(Integer page,
                                                           Integer size,
                                                           String sort,
                                                           String tag,
                                                           String urlContains,
                                                           OffsetDateTime lastCheckBefore,
                                                           OffsetDateTime lastCheckAfter,
                                                           Long chatId);

}
