package backend.academy.linktracker.bot.dto;

import java.util.List;

public record ListLinksResponse(List<LinkResponse> links, Integer size) {}
