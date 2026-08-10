package backend.academy.linktracker.ai.config.logging;
import backend.academy.linktracker.ai.config.logging.properties.HttpMaskingProperties;
import backend.academy.linktracker.ai.service.MaskingService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class LoggingConfig {

    @Bean
    public FilterRegistrationBean<HttpLoggingFilter> loggingFilter(MaskingService maskingService, HttpMaskingProperties httpMaskingProperties) {
        FilterRegistrationBean<HttpLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HttpLoggingFilter(maskingService, httpMaskingProperties));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
