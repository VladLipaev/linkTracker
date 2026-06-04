package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.controller.BotIpRateLimitingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BotIpRateLimitingFilterConfig {

    @Bean
    public FilterRegistrationBean<BotIpRateLimitingFilter> rateLimitingFilter(BotIpRateLimitingFilter filter) {
        FilterRegistrationBean<BotIpRateLimitingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/updates/*");
        return registrationBean;
    }
}
