package backend.academy.linktracker.scrapper.service.mapper;

import backend.academy.linktracker.scrapper.dto.ChatAndListLinksResponse;
import backend.academy.linktracker.scrapper.entity.Chat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = SubscriptionLinkResponseMapper.class)
public interface ChatAndListLinksResponseMapper {

    @Mapping(source = "id", target = "chatId")
    @Mapping(source = "subscriptions", target = "links")
    ChatAndListLinksResponse toChatAndListLinksResponse(Chat chat);


}
