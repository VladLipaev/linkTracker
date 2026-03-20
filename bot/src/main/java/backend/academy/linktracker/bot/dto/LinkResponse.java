package backend.academy.linktracker.bot.dto;

import java.util.List;

public record LinkResponse(Long id, String url, List<String> tags) {}
