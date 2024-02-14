package com.intuit.springwebclient.config;


import com.intuit.springwebclient.filter.WebClientRequestFilter;
import com.intuit.springwebclient.util.WebClientConstants;
import io.netty.channel.ChannelOption;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * Define the instance of Spring 5 Web Client interface
 */
@Configuration
@AllArgsConstructor
public class WebClientConfiguration {
    private final SpringWebClientConfig webClientConfiguration;
    private final WebClientRequestFilter webClientRequestFilter;


    @Bean
    public ConnectionProvider webClientConnectionProvider(){
        return ConnectionProvider.builder(WebClientConstants.CONNECTION_PROVIDER_NAME)
                        .maxConnections(webClientConfiguration.getConnectionPool().getMaxConnections())
                        .maxIdleTime(Duration.ofMillis(webClientConfiguration.getConnectionPool().getMaxIdleTime()))
                        .maxLifeTime(Duration.ofMillis(webClientConfiguration.getConnectionPool().getMaxLifeTime()))
                        .pendingAcquireTimeout(Duration.ofMillis(webClientConfiguration.getConnectionPool().getPendingAcquireTimeout()))
                        .build();
    }

    @Bean
    public HttpClient webHttpClient(){
        return HttpClient.create(webClientConnectionProvider())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, webClientConfiguration.getHttpClientConfig().getConnectTimeoutMillis())
                .option(ChannelOption.SO_TIMEOUT, webClientConfiguration.getHttpClientConfig().getSocketTimeoutMillis());
    }

    @Bean("RWebPulseClient")
    public WebClient createWebClient() {

        WebClient.Builder builder = WebClient.builder();
        builder.clientConnector(new ReactorClientHttpConnector(webHttpClient()))
                .filter(webClientRequestFilter.getFilter());
        //if max-in-memory-size is not set in config then the building client with default size else creating client with custom max-in-memory-size
        if (webClientConfiguration.getMaxInMemorySize() > 0) {
            builder.exchangeStrategies(
                            ExchangeStrategies.builder()
                                    .codecs(configurer -> configurer
                                            .defaultCodecs()
                                            .maxInMemorySize(webClientConfiguration.getMaxInMemorySize()))
                                    .build());
        }
        return builder.build();
    }
}
