package backend.academy.linktracker.scrapper.config;

import backend.academy.linktracker.scrapper.controller.ScrapperIpRateLimitingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScrapperIpRateLimitingFilterConfig {

    @Bean
    public FilterRegistrationBean<ScrapperIpRateLimitingFilter> rateLimitingFilter(
            ScrapperIpRateLimitingFilter filter) {
        FilterRegistrationBean<ScrapperIpRateLimitingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/links/*", "/tg-chat/*");
        return registrationBean;
    }
}
