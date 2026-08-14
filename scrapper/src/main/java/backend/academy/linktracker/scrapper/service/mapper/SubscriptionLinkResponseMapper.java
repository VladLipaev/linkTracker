package backend.academy.linktracker.scrapper.service.mapper;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UriConverter.class)
public interface SubscriptionLinkResponseMapper {

    @Mapping(source = "link.id", target = "id")
    @Mapping(source = "link.url", target = "url")
    @Mapping(source = "link.lastCheckedAt", target = "lastCheckedAt")
    @Mapping(source = "tags", target = "tags")
    LinkResponse toLinkResponse(Subscription subscription);
}
