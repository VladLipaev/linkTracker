package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.PageSubscriptionsResponse;
import backend.academy.linktracker.scrapper.entity.Subscription;
import backend.academy.linktracker.scrapper.repository.orm.JpaSubscriptionRepositoryInvoker;
import backend.academy.linktracker.scrapper.repository.utils.ParseSortUtil;
import backend.academy.linktracker.scrapper.repository.utils.SubscriptionSpecifications;
import backend.academy.linktracker.scrapper.service.mapper.SubscriptionToSubscriptionResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DefaultSubscriptionService implements SubscriptionService {

    private final JpaSubscriptionRepositoryInvoker jpaSubscriptionRepositoryInvoker;
    private final SubscriptionToSubscriptionResponseMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PageSubscriptionsResponse getPageSubscriptionsByFilter(Integer page,
                                                                  Integer size,
                                                                  String sort,
                                                                  String tag,
                                                                  String urlContains,
                                                                  OffsetDateTime lastCheckBefore,
                                                                  OffsetDateTime lastCheckAfter,
                                                                  Long chatId) {
        Specification<Subscription> spec = Specification
            .where(SubscriptionSpecifications.hasTag(tag))
            .and(SubscriptionSpecifications.chatIdEquals(chatId))
            .and(SubscriptionSpecifications.urlContains(urlContains))
            .and(SubscriptionSpecifications.updatedAfter(
                lastCheckAfter != null ? lastCheckAfter.toInstant() : null))
            .and(SubscriptionSpecifications.updatedBefore(
                lastCheckBefore != null ? lastCheckBefore.toInstant() : null));

        Pageable pageable = PageRequest.of(page, size, ParseSortUtil.parseSort(sort));
        Page<Subscription> pageSubscriptions = jpaSubscriptionRepositoryInvoker.findAll(spec, pageable);


        return PageSubscriptionsResponse.builder()
            .subscriptions(pageSubscriptions.getContent().stream().map(mapper::toResponse).toList())
            .empty(pageSubscriptions.isEmpty())
            .first(pageSubscriptions.isFirst())
            .last(pageSubscriptions.isLast())
            .number(pageSubscriptions.getNumber())
            .numberOfElements(pageSubscriptions.getNumberOfElements())
            .size(pageSubscriptions.getSize())
            .totalElements(pageSubscriptions.getTotalElements())
            .totalPages(pageSubscriptions.getTotalPages())
            .build();
    }
}
