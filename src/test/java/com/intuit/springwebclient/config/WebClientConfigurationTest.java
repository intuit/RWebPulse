package com.intuit.springwebclient.config;

import com.intuit.springwebclient.filter.WebClientRequestFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.netty.resources.ConnectionProvider;

@ExtendWith(MockitoExtension.class)
class WebClientConfigurationTest {

  @Mock
  private SpringWebClientConfig springWebClientConfig;
  @Mock
  private WebClientRequestFilter webClientRequestFilter;

  @InjectMocks
  private WebClientConfiguration webClientConfiguration;

  @Test
  void testWebClientConnectionProvider() {
    HttpConnectionPoolConfig poolConfig = new HttpConnectionPoolConfig();
    poolConfig.setMaxConnections(200);
    poolConfig.setMaxIdleTime(2000L);
    poolConfig.setMaxLifeTime(3500L);
    poolConfig.setPendingAcquireTimeout(1500L);

    Mockito.when(springWebClientConfig.getConnectionPool()).thenReturn(poolConfig);

    ConnectionProvider connectionProvider = webClientConfiguration.webClientConnectionProvider();
    Assertions.assertEquals(200, connectionProvider.maxConnections());

    HttpClientConfig httpClientConfig = new HttpClientConfig();
    httpClientConfig.setConnectTimeoutMillis(5000);
    httpClientConfig.setSocketTimeoutMillis(3000);
    httpClientConfig.setMaxTTLForRoute("testHost", 6000); // Adding a TTL for a sample route

    Mockito.when(springWebClientConfig.getHttpClientConfig()).thenReturn(httpClientConfig);

    Mockito.when(webClientRequestFilter.getFilter()).thenReturn(new ExchangeFilterFunction() {
      @Override
      public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return null;
      }
    });

    WebClient webClient = webClientConfiguration.createWebClient();
  }
}
