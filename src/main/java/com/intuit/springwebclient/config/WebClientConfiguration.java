package com.intuit.springwebclient.config;

import com.intuit.springwebclient.filter.WebClientRequestFilter;
import com.intuit.springwebclient.util.WebClientConstants;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.IdleStateHandler;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
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
  public ConnectionProvider webClientConnectionProvider() {
    return ConnectionProvider.builder(WebClientConstants.CONNECTION_PROVIDER_NAME)
        .maxConnections(webClientConfiguration.getConnectionPool().getMaxConnections())
        .maxIdleTime(Duration.ofMillis(webClientConfiguration.getConnectionPool().getMaxIdleTime()))
        .maxLifeTime(Duration.ofMillis(webClientConfiguration.getConnectionPool().getMaxLifeTime()))
        .pendingAcquireTimeout(Duration.ofMillis(
            webClientConfiguration.getConnectionPool().getPendingAcquireTimeout()))
        .build();
  }

  @Bean
  public HttpClient webHttpClient() {
    return HttpClient.create(webClientConnectionProvider())
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
            webClientConfiguration.getHttpClientConfig().getConnectTimeoutMillis())
        .option(ChannelOption.SO_TIMEOUT,
            webClientConfiguration.getHttpClientConfig().getSocketTimeoutMillis())
        .doOnConnected(connection -> {
          String route = null;
          if (connection.address() instanceof InetSocketAddress) {
            route = ((InetSocketAddress) connection.address()).getHostName();
          }

          if (route != null) {
            Integer ttlForRoute = webClientConfiguration.getHttpClientConfig()
                .getMaxTTLForRoute(route);
            if (ttlForRoute != null) {
              connection.addHandlerLast(
                  new IdleStateHandler(ttlForRoute, 0, 0, TimeUnit.MILLISECONDS));
            }
          }
        });
  }

  @Bean
  public WebClient createWebClient() {
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(webHttpClient()))
        .filter(webClientRequestFilter.getFilter())
        .build();
  }
}
