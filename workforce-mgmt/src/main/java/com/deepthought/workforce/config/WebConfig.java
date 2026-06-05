package com.deepthought.workforce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * LF-203: Enforce max page size so no one can request 1 million rows via ?size=1000000
 */
@Configuration
public class WebConfig {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setMaxPageSize(100);
            resolver.setFallbackPageable(org.springframework.data.domain.PageRequest.of(0, 20));
        };
    }
}
