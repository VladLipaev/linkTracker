package backend.academy.linktracker.scrapper.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@ConfigurationProperties(prefix = "logging.masking.so")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class SOMaskingProperties implements MaskingProperties {

    private List<String> headers = List.of();

    private List<String> keys = List.of("key");
}
