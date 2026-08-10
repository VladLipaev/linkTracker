package backend.academy.linktracker.ai.config.logging.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@ConfigurationProperties(prefix = "logging.ai-agent.masking.yandex")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class YandexMaskingProperties implements MaskingProperties {

    private List<String> headers = List.of("Authorization", "x-folder-id");

    private List<String> keys = List.of();
}
