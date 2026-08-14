package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.api.ChatsApi;
import backend.academy.linktracker.scrapper.dto.ChatsPageResponse;
import backend.academy.linktracker.scrapper.service.TgChatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class TgChatsRestController implements ChatsApi {

    private final TgChatsService tgChatsService;

    @Override
    public ResponseEntity<ChatsPageResponse> getChats(Pageable pageable) {
        return ResponseEntity.ok(tgChatsService.getChats(pageable));
    }
}
