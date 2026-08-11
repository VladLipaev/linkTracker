package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.ChatSummary;
import backend.academy.linktracker.scrapper.dto.ChatsPageResponse;
import backend.academy.linktracker.scrapper.repository.orm.JpaTgChatRepositoryInvoker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TgChatsService {

    private final JpaTgChatRepositoryInvoker repository;

    public ChatsPageResponse getChats(Pageable pageable) {
        Page<ChatSummary> page = repository.getChatsAndSubsSize(pageable);
        List<ChatSummary> content = page.getContent();
        return ChatsPageResponse.builder()
            .content(content)
            .totalPages(page.getTotalPages())
            .totalElements(page.getTotalElements())
            .number(page.getNumber())
            .size(page.getSize())
            .numberOfElements(page.getNumberOfElements())
            .first(page.isFirst())
            .last(page.isLast())
            .empty(page.isEmpty())
            .build();
    }
}
