package backend.academy.linktracker.bot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record LinkUpdate(
        @NotNull Long id,

        @NotNull @NotBlank String url,

        @NotNull @NotBlank String description,

        @NotNull List<Long> tgChatIds) {}
