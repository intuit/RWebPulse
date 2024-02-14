package com.intuit.springwebclient.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring-web-client-config")
@Data
public class SpringWebClientConfig {
    private HttpConnectionPoolConfig connectionPool;
    private HttpClientConfig httpClientConfig;
    private int maxInMemorySize;
}
