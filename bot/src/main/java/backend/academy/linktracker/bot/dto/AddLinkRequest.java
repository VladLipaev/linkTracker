package backend.academy.linktracker.bot.dto;

import java.util.List;

public record AddLinkRequest(String link, List<String> tags) {}
