package backend.academy.linktracker.scrapper.dto;

import java.util.List;

public record LinkResponse(Long id, String url, List<String> tags) {}
