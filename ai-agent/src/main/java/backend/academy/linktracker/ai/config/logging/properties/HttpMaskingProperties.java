package backend.academy.linktracker.ai.config.logging.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@ConfigurationProperties(prefix = "logging.ai-agent.masking.http")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class HttpMaskingProperties implements MaskingProperties {

    private List<String> headers = List.of("Authorization");

    private List<String> keys = List.of();
}
