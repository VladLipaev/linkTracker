package backend.academy.linktracker.scrapper.config.logging;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class LoggingConfig {

    @Bean
    public FilterRegistrationBean<HttpLoggingFilter> loggingFilter() {
        FilterRegistrationBean<HttpLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HttpLoggingFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
