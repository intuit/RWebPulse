package com.intuit.springwebclient.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intuit.springwebclient.config.WebClientRetryConfig;
import com.intuit.springwebclient.entity.ClientHttpRequest;
import com.intuit.springwebclient.entity.ClientHttpResponse;
import com.intuit.springwebclient.retryHandler.RetryHandler;
import com.intuit.springwebclient.retryHandler.RetryHandlerFactory;
import java.util.Arrays;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.UnknownContentTypeException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class CommonSpringWebClientTest {

  @Mock
  private WebClient webClient;
  @Mock
  private WebClient.RequestBodyUriSpec requestBodyUriSpec;
  @Mock
  private WebClient.RequestBodySpec requestBodySpec;
  @Mock
  private WebClient.RequestHeadersSpec headersSpec;
  @Mock
  private WebClient.ResponseSpec responseSpec;
  @Mock
  private RetryHandler retryHandler;

  @InjectMocks
  private CommonSpringWebClient commonSpringWebClient;

  @BeforeEach
  void setUp() {
    // Clear MDC before each test
    MDC.clear();

    // Setup default retry handler mock
    lenient().when(retryHandler.getName()).thenReturn("testHandler");
    lenient().doNothing().when(retryHandler).checkAndThrowRetriableException(any(Exception.class));
  }

  @Test
  public void testSyncHttpResponseSuccess() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest().build();
    setupWebClientMock();
    mockSuccessfulResponse();

    // When
    ClientHttpResponse<String> response = commonSpringWebClient.syncHttpResponse(clientHttpRequest);

    // Then
    assertNotNull(response);
    assertTrue(response.isSuccess2xx());
    assertEquals("test-response", response.getResponse());
  }

  @Test
  public void testAsyncHttpResponseSuccess() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest().build();
    setupWebClientMock();
    mockSuccessfulResponse();

    // When & Then
    StepVerifier.create(commonSpringWebClient.asyncHttpResponse(clientHttpRequest))
        .expectNextMatches(response ->
            response.isSuccess2xx() && "test-response".equals(response.getResponse()))
        .verifyComplete();
  }

  @Test
  public void testSyncHttpResponseSuccessNoRequestBody() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest().request(null)
        .requestType(null).build();
    setupWebClientMockNoBody();
    mockSuccessfulResponse();

    // When
    ClientHttpResponse<String> response = commonSpringWebClient.syncHttpResponse(clientHttpRequest);

    // Then
    assertNotNull(response);
    assertTrue(response.isSuccess2xx());
  }

  @Test
  public void testAsyncHttpResponseSuccessNoRequestBody() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest().request(null)
        .requestType(null).build();
    setupWebClientMockNoBody();
    mockSuccessfulResponse();

    // When & Then
    StepVerifier.create(commonSpringWebClient.asyncHttpResponse(clientHttpRequest))
        .expectNextMatches(response -> response.isSuccess2xx())
        .verifyComplete();
  }

  @Test
  public void testSyncHttpResponseWithMdcContext() {
    // Given
    MDC.put("test-key", "test-value");
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest().build();
    setupWebClientMock();
    mockSuccessfulResponse();

    // When
    ClientHttpResponse<String> response = commonSpringWebClient.syncHttpResponse(clientHttpRequest);

    // Then
    assertNotNull(response);
    assertTrue(response.isSuccess2xx());
    // MDC should be cleared after the operation
    assertTrue(MDC.get("test-key") == null);
  }

  @Test
  public void testAsyncHttpResponseWithMdcContext() {
    // Given
    MDC.put("test-key", "test-value");
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest().build();
    setupWebClientMock();
    mockSuccessfulResponse();

    // When & Then
    StepVerifier.create(commonSpringWebClient.asyncHttpResponse(clientHttpRequest))
        .expectNextMatches(response -> response.isSuccess2xx())
        .verifyComplete();

    // MDC should be cleared after the operation
    assertTrue(MDC.get("test-key") == null);
  }

  @Test
  public void testSyncHttpResponseWithRetryConfig() {
    // Given
    WebClientRetryConfig retryConfig = WebClientRetryConfig.builder()
        .maxAttempts(3)
        .backOff(1)
        .retryFilter(ex -> ex instanceof WebClientResponseException)
        .build();

    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest()
        .clientRetryConfig(retryConfig)
        .build();

    setupWebClientMock();
    mockSuccessfulResponse();

    // When
    ClientHttpResponse<String> response = commonSpringWebClient.syncHttpResponse(clientHttpRequest);

    // Then
    assertNotNull(response);
    assertTrue(response.isSuccess2xx());
  }

  @Test
  public void testAsyncHttpResponseWithRetryConfig() {
    // Given
    WebClientRetryConfig retryConfig = WebClientRetryConfig.builder()
        .maxAttempts(3)
        .backOff(1)
        .retryFilter(ex -> ex instanceof WebClientResponseException)
        .build();

    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest()
        .clientRetryConfig(retryConfig)
        .build();

    setupWebClientMock();
    mockSuccessfulResponse();

    // When & Then
    StepVerifier.create(commonSpringWebClient.asyncHttpResponse(clientHttpRequest))
        .expectNextMatches(response -> response.isSuccess2xx())
        .verifyComplete();
  }

  @Test
  public void testSyncHttpResponseWebClientResponseException() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest().build();
    setupWebClientMock();
    mockWebClientResponseException();

    // Mock static RetryHandlerFactory
    try (MockedStatic<RetryHandlerFactory> mockedRetryHandlerFactory = Mockito.mockStatic(
        RetryHandlerFactory.class)) {
      mockedRetryHandlerFactory.when(() -> RetryHandlerFactory.getHandler(anyString()))
          .thenReturn(retryHandler);

      // When
      ClientHttpResponse<String> response = commonSpringWebClient.syncHttpResponse(
          clientHttpRequest);

      // Then
      assertNotNull(response);
      assertTrue(!response.isSuccess2xx());
      assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }
  }

  @Test
  public void testAsyncHttpResponseWebClientResponseException() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest().build();
    setupWebClientMock();
    mockWebClientResponseException();

    // Mock static RetryHandlerFactory
    try (MockedStatic<RetryHandlerFactory> mockedRetryHandlerFactory = Mockito.mockStatic(
        RetryHandlerFactory.class)) {
      mockedRetryHandlerFactory.when(() -> RetryHandlerFactory.getHandler(anyString()))
          .thenReturn(retryHandler);

      // When & Then
      StepVerifier.create(commonSpringWebClient.asyncHttpResponse(clientHttpRequest))
          .expectNextMatches(response ->
              !response.isSuccess2xx() && HttpStatus.NOT_FOUND.equals(response.getStatus()))
          .verifyComplete();
    }
  }

  @Test
  public void testSyncHttpResponseHttpStatusCodeException() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest().build();
    setupWebClientMock();
    mockHttpStatusCodeException();

    // Mock static RetryHandlerFactory
    try (MockedStatic<RetryHandlerFactory> mockedRetryHandlerFactory = Mockito.mockStatic(
        RetryHandlerFactory.class)) {
      mockedRetryHandlerFactory.when(() -> RetryHandlerFactory.getHandler(anyString()))
          .thenReturn(retryHandler);

      // When
      ClientHttpResponse<String> response = commonSpringWebClient.syncHttpResponse(
          clientHttpRequest);

      // Then
      assertNotNull(response);
      assertTrue(!response.isSuccess2xx());
      assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }
  }

  @Test
  public void testAsyncHttpResponseHttpStatusCodeException() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest().build();
    setupWebClientMock();
    mockHttpStatusCodeException();

    // Mock static RetryHandlerFactory
    try (MockedStatic<RetryHandlerFactory> mockedRetryHandlerFactory = Mockito.mockStatic(
        RetryHandlerFactory.class)) {
      mockedRetryHandlerFactory.when(() -> RetryHandlerFactory.getHandler(anyString()))
          .thenReturn(retryHandler);

      // When & Then
      StepVerifier.create(commonSpringWebClient.asyncHttpResponse(clientHttpRequest))
          .expectNextMatches(response ->
              !response.isSuccess2xx() && HttpStatus.NOT_FOUND.equals(response.getStatus()))
          .verifyComplete();
    }
  }

  @Test
  public void testSyncHttpResponseUnknownContentTypeException() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest().build();
    setupWebClientMock();
    mockUnknownContentTypeException();

    // Mock static RetryHandlerFactory
    try (MockedStatic<RetryHandlerFactory> mockedRetryHandlerFactory = Mockito.mockStatic(
        RetryHandlerFactory.class)) {
      mockedRetryHandlerFactory.when(() -> RetryHandlerFactory.getHandler(anyString()))
          .thenReturn(retryHandler);

      // When
      ClientHttpResponse<String> response = commonSpringWebClient.syncHttpResponse(
          clientHttpRequest);

      // Then
      assertNotNull(response);
      assertTrue(!response.isSuccess2xx());
      assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatus());
    }
  }

  @Test
  public void testAsyncHttpResponseUnknownContentTypeException() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest().build();
    setupWebClientMock();
    mockUnknownContentTypeException();

    // Mock static RetryHandlerFactory
    try (MockedStatic<RetryHandlerFactory> mockedRetryHandlerFactory = Mockito.mockStatic(
        RetryHandlerFactory.class)) {
      mockedRetryHandlerFactory.when(() -> RetryHandlerFactory.getHandler(anyString()))
          .thenReturn(retryHandler);

      // When & Then
      StepVerifier.create(commonSpringWebClient.asyncHttpResponse(clientHttpRequest))
          .expectNextMatches(response ->
              !response.isSuccess2xx() && HttpStatus.UNSUPPORTED_MEDIA_TYPE.equals(
                  response.getStatus()))
          .verifyComplete();
    }
  }

  @Test
  public void testSyncHttpResponseGenericException() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest().build();
    setupWebClientMock();
    mockGenericException();

    // Mock static RetryHandlerFactory
    try (MockedStatic<RetryHandlerFactory> mockedRetryHandlerFactory = Mockito.mockStatic(
        RetryHandlerFactory.class)) {
      mockedRetryHandlerFactory.when(() -> RetryHandlerFactory.getHandler(anyString()))
          .thenReturn(retryHandler);

      // When
      ClientHttpResponse<String> response = commonSpringWebClient.syncHttpResponse(
          clientHttpRequest);

      // Then
      assertNotNull(response);
      assertTrue(!response.isSuccess2xx());
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
    }
  }

  @Test
  public void testAsyncHttpResponseGenericException() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest().build();
    setupWebClientMock();
    mockGenericException();

    // Mock static RetryHandlerFactory
    try (MockedStatic<RetryHandlerFactory> mockedRetryHandlerFactory = Mockito.mockStatic(
        RetryHandlerFactory.class)) {
      mockedRetryHandlerFactory.when(() -> RetryHandlerFactory.getHandler(anyString()))
          .thenReturn(retryHandler);

      // When & Then
      StepVerifier.create(commonSpringWebClient.asyncHttpResponse(clientHttpRequest))
          .expectNextMatches(response ->
              !response.isSuccess2xx() && HttpStatus.INTERNAL_SERVER_ERROR.equals(
                  response.getStatus()))
          .verifyComplete();
    }
  }

  @Test
  public void testSyncHttpResponseWithRetryHandlers() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest()
        .retryHandlers(Arrays.asList("testHandler"))
        .build();
    setupWebClientMock();
    mockSuccessfulResponse();

    // When
    ClientHttpResponse<String> response = commonSpringWebClient.syncHttpResponse(clientHttpRequest);

    // Then
    assertNotNull(response);
    assertTrue(response.isSuccess2xx());
  }

  @Test
  public void testAsyncHttpResponseWithRetryHandlers() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest()
        .retryHandlers(Arrays.asList("testHandler"))
        .build();
    setupWebClientMock();
    mockSuccessfulResponse();

    // When & Then
    StepVerifier.create(commonSpringWebClient.asyncHttpResponse(clientHttpRequest))
        .expectNextMatches(response -> response.isSuccess2xx())
        .verifyComplete();
  }

  @Test
  public void testSyncHttpResponseWithRetryHandlersAndStaticMock() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest()
        .retryHandlers(Arrays.asList("testHandler"))
        .build();
    setupWebClientMock();
    mockSuccessfulResponse();

    // Mock static RetryHandlerFactory
    try (MockedStatic<RetryHandlerFactory> mockedRetryHandlerFactory = Mockito.mockStatic(
        RetryHandlerFactory.class)) {
      mockedRetryHandlerFactory.when(() -> RetryHandlerFactory.getHandler("testHandler"))
          .thenReturn(retryHandler);

      // When
      ClientHttpResponse<String> response = commonSpringWebClient.syncHttpResponse(
          clientHttpRequest);

      // Then
      assertNotNull(response);
      assertTrue(response.isSuccess2xx());
      // No verify here
    }
  }

  @Test
  public void testAsyncHttpResponseWithRetryHandlersAndStaticMock() {
    // Given
    ClientHttpRequest<String, String> clientHttpRequest = createClientHttpRequest()
        .retryHandlers(Arrays.asList("testHandler"))
        .build();
    setupWebClientMock();
    mockSuccessfulResponse();

    // Mock static RetryHandlerFactory
    try (MockedStatic<RetryHandlerFactory> mockedRetryHandlerFactory = Mockito.mockStatic(
        RetryHandlerFactory.class)) {
      mockedRetryHandlerFactory.when(() -> RetryHandlerFactory.getHandler("testHandler"))
          .thenReturn(retryHandler);

      // When & Then
      StepVerifier.create(commonSpringWebClient.asyncHttpResponse(clientHttpRequest))
          .expectNextMatches(response -> response.isSuccess2xx())
          .verifyComplete();
      // No verify here
    }
  }

  // Helper methods for mocking
  private void setupWebClientMock() {
    // Mock the complete WebClient chain with body
    lenient().when(webClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
    lenient().when(requestBodyUriSpec.uri("test-url")).thenReturn(requestBodyUriSpec);
    lenient().when(requestBodyUriSpec.headers(any())).thenReturn(requestBodySpec);
    lenient().when(requestBodySpec.body(any(Mono.class), any(ParameterizedTypeReference.class)))
        .thenReturn(headersSpec);
    lenient().when(headersSpec.retrieve()).thenReturn(responseSpec);
    // Also mock retrieve() directly on requestBodySpec for no-body path
    lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
  }

  private void setupWebClientMockNoBody() {
    // Mock the complete WebClient chain without body
    lenient().when(webClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
    lenient().when(requestBodyUriSpec.uri("test-url")).thenReturn(requestBodyUriSpec);
    lenient().when(requestBodyUriSpec.headers(any())).thenReturn(requestBodySpec);
    lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
  }

  private void mockSuccessfulResponse() {
    ResponseEntity<String> responseEntity = ResponseEntity.ok("test-response");
    when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
        .thenReturn(Mono.just(responseEntity));
  }

  private void mockWebClientResponseException() {
    WebClientResponseException exception = mock(WebClientResponseException.class);
    when(exception.getResponseBodyAsString()).thenReturn("Not Found");
    when(exception.getHeaders()).thenReturn(new HttpHeaders());
    when(exception.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

    when(responseSpec.toEntity(ParameterizedTypeReference.forType(String.class)))
        .thenReturn(Mono.error(exception));
  }

  private void mockHttpStatusCodeException() {
    HttpClientErrorException exception = mock(HttpClientErrorException.class);
    when(exception.getResponseBodyAsString()).thenReturn("Not Found");
    when(exception.getResponseHeaders()).thenReturn(new HttpHeaders());
    when(exception.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

    when(responseSpec.toEntity(ParameterizedTypeReference.forType(String.class)))
        .thenReturn(Mono.error(exception));
  }

  private void mockUnknownContentTypeException() {
    UnknownContentTypeException exception = mock(UnknownContentTypeException.class);
    when(exception.getResponseBodyAsString()).thenReturn("Unsupported Media Type");
    when(exception.getResponseHeaders()).thenReturn(new HttpHeaders());
    when(exception.getRawStatusCode()).thenReturn(415);

    when(responseSpec.toEntity(ParameterizedTypeReference.forType(String.class)))
        .thenReturn(Mono.error(exception));
  }

  private void mockGenericException() {
    when(responseSpec.toEntity(ParameterizedTypeReference.forType(String.class)))
        .thenReturn(Mono.error(new IllegalArgumentException("Test exception")));
  }

  private ClientHttpRequest.ClientHttpRequestBuilder<String, String> createClientHttpRequest() {
    HttpHeaders httpHeadersMock = new HttpHeaders();
    Consumer<HttpHeaders> httpHeadersConsumer = new Consumer<HttpHeaders>() {
      @Override
      public void accept(HttpHeaders httpHeaders) {
        return;
      }
    };
    httpHeadersConsumer.accept(httpHeadersMock);

    return ClientHttpRequest.<String, String>builder()
        .httpMethod(HttpMethod.GET)
        .url("test-url")
        .requestHeaders(httpHeadersMock)
        .requestType(ParameterizedTypeReference.forType(String.class))
        .request("hello")
        .responseType(ParameterizedTypeReference.forType(String.class))
        .clientRetryConfig(WebClientRetryConfig.builder()
            .maxAttempts(0) // Disable retries for testing
            .backOff(1)
            .retryFilter(ex -> false)
            .build());
  }
}
