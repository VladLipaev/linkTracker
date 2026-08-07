package backend.academy.linktracker.bot.configuration.logging;
import backend.academy.linktracker.bot.properties.HttpMaskingProperties;
import backend.academy.linktracker.bot.service.MaskingService;
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
