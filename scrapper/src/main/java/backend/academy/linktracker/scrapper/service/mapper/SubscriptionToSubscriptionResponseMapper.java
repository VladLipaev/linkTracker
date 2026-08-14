package backend.academy.linktracker.scrapper.service.mapper;

import backend.academy.linktracker.scrapper.dto.SubscriptionResponse;
import backend.academy.linktracker.scrapper.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UriConverter.class)
public interface SubscriptionToSubscriptionResponseMapper {

    @Mapping(source = "chat.id", target = "chatId")
    @Mapping(source = "link.id", target = "linkId")
    @Mapping(source = "tags", target = "tags")
    @Mapping(source = "link.url", target = "url")
    @Mapping(source = "link.lastCheckedAt", target = "lastCheckedAt")
    SubscriptionResponse toResponse(Subscription subscription);
}
