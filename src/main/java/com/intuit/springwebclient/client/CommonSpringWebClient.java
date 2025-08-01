package com.intuit.springwebclient.client;

import com.intuit.springwebclient.entity.ClientHttpRequest;
import com.intuit.springwebclient.entity.ClientHttpResponse;
import com.intuit.springwebclient.retryHandler.RetryHandlerFactory;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Spring 5 Web Client Method executor with MDC propagation for retries.
 */
@Slf4j
@Component
public class CommonSpringWebClient {

  private final WebClient webClient;

  // --- Constant for MDC Context Key ---
  // This key is used to store and retrieve the MDC map from Reactor's Context
  private static final String MDC_CONTEXT_KEY = "mdcContextMap";

  public CommonSpringWebClient(@Qualifier("RWebPulseClient") WebClient webClient) {
    this.webClient = webClient;
  }

  /**
   * Executes a blocking HTTP request with WebClient, supporting retries and MDC propagation across
   * thread changes.
   *
   * @param httpRequest The client HTTP request details.
   * @param <REQUEST>   Type of the request body.
   * @param <RESPONSE>  Type of the response body.
   * @return ClientHttpResponse containing the response or error details.
   */
  public <REQUEST, RESPONSE> ClientHttpResponse<RESPONSE> syncHttpResponse(
      ClientHttpRequest<REQUEST, RESPONSE> httpRequest) {
    return asyncHttpResponse(httpRequest).block();
  }

  /**
   * Executes a non-blocking HTTP request with WebClient, supporting retries and MDC propagation
   * across thread changes. Returns a Mono for reactive programming.
   *
   * @param httpRequest The client HTTP request details.
   * @param <REQUEST>   Type of the request body.
   * @param <RESPONSE>  Type of the response body.
   * @return Mono<ClientHttpResponse < RESPONSE>> containing the response or error details.
   */
  public <REQUEST, RESPONSE> Mono<ClientHttpResponse<RESPONSE>> asyncHttpResponse(
      ClientHttpRequest<REQUEST, RESPONSE> httpRequest) {
    final Map<String, String> mdcContextMap = MDC.getCopyOfContextMap();
    log.info("asyncHttpResponse initiated. Captured MDC from calling thread: {}", mdcContextMap);

    // --- Build the Reactive Chain ---
    return generateResponseSpec(httpRequest)
        .toEntity(httpRequest.getResponseType())
        .map(this::generateResponse)
        .retryWhen(generateRetrySpec(httpRequest))
        .contextWrite(ctx -> {
          if (mdcContextMap != null) {
            return ctx.put(MDC_CONTEXT_KEY, mdcContextMap);
          }
          return ctx;
        })
        .doOnEach(signal -> {
          Optional<Map<String, String>> contextFromReactor = signal.getContextView()
              .getOrEmpty(MDC_CONTEXT_KEY);
          contextFromReactor.ifPresent(MDC::setContextMap);
        })
        .onErrorResume(WebClientResponseException.class, ex -> {
          final String errorMessage = String.format(
              "Error in WebClient call (ResponseException). Error=%s Headers=%s statusCode=%s",
              ex.getResponseBodyAsString(), ex.getHeaders(), ex.getStatusCode());
          return Mono.just(handleExceptionInternal(ex, errorMessage, ex.getResponseBodyAsString(),
              HttpStatus.valueOf(ex.getStatusCode().value()), httpRequest));
        })
        .onErrorResume(org.springframework.web.client.HttpStatusCodeException.class, ex -> {
          final String errorMessage = String.format(
              "Error in WebClient call (HttpStatusCodeException). Error=%s Headers=%s statusCode=%s",
              ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode());
          return Mono.just(handleExceptionInternal(ex, errorMessage, ex.getResponseBodyAsString(),
              HttpStatus.valueOf(ex.getStatusCode().value()), httpRequest));
        })
        .onErrorResume(org.springframework.web.client.UnknownContentTypeException.class, ex -> {
          final String errorMessage = String.format(
              "Error in WebClient call (UnknownContentTypeException). Error=%s Headers=%s",
              ex.getResponseBodyAsString(), ex.getResponseHeaders());
          return Mono.just(handleExceptionInternal(ex, errorMessage, ex.getResponseBodyAsString(),
              HttpStatus.valueOf(ex.getRawStatusCode()), httpRequest));
        })
        .onErrorResume(Exception.class, ex -> { // Catch any other unexpected exceptions
          final String errorMessage = String.format(
              "Unhandled exception in WebClient call. Error=%s Cause=%s", ex.getMessage(),
              ex.getCause());
          return Mono.just(
              handleExceptionInternal(ex, errorMessage, null, HttpStatus.INTERNAL_SERVER_ERROR,
                  httpRequest));
        })
        .doFinally(signalType -> {
          MDC.clear();
          log.info("MDC cleared after reactive chain completion (signal type: {}).", signalType);
        });
  }

  /**
   * Generates WebClient ResponseSpec from the ClientHttpRequest.
   *
   * @param httpRequest The client HTTP request details.
   * @return WebClient.ResponseSpec ready for retrieval.
   */
  private <REQUEST, RESPONSE> WebClient.ResponseSpec generateResponseSpec(
      ClientHttpRequest<REQUEST, RESPONSE> httpRequest) {

    Consumer<HttpHeaders> httpHeadersConsumer = (httpHeaders -> httpHeaders
        .putAll(httpRequest.getRequestHeaders()));
    RequestBodySpec webClientBuilder = webClient.method(httpRequest.getHttpMethod())
        .uri(httpRequest.getUrl())
        .headers(httpHeadersConsumer);

    if (Objects.nonNull(httpRequest.getRequest()) && Objects.nonNull(
        httpRequest.getRequestType())) {
      webClientBuilder.body(Mono.just(httpRequest.getRequest()), httpRequest.getRequestType());
    }

    return webClientBuilder.retrieve();
  }

  /**
   * Generates retry specification for the request based on config provided.
   *
   * @param httpRequest The client HTTP request details including retry configuration.
   * @return Reactor Retry specification.
   */
  private <REQUEST, RESPONSE> Retry generateRetrySpec(
      ClientHttpRequest<REQUEST, RESPONSE> httpRequest) {
    return Retry
        .fixedDelay(httpRequest.getClientRetryConfig().getMaxAttempts(),
            Duration.ofSeconds(httpRequest.getClientRetryConfig().getBackOff()))
        .doBeforeRetry(signal -> {
          log.info("Retrying for requestUrl={}, retryCount={}",
              httpRequest.getUrl(), signal.totalRetries());
        })
        .filter(httpRequest.getClientRetryConfig().getRetryFilter());
  }

  /**
   * Handles a successful HTTP response, transforming it into ClientHttpResponse.
   *
   * @param response   The ResponseEntity from WebClient.
   * @param <RESPONSE> Type of the response body.
   * @return ClientHttpResponse indicating success.
   */
  private <RESPONSE> ClientHttpResponse<RESPONSE> generateResponse(
      ResponseEntity<RESPONSE> response) {
    return ClientHttpResponse.<RESPONSE>builder().response(response.getBody())
        .status(response.getStatusCode())
        .isSuccess2xx(response.getStatusCode().is2xxSuccessful()).build();
  }

  /**
   * Internal method to handle exceptions and build an error ClientHttpResponse. This is now called
   * from within the `onErrorResume` operators in the reactive chain.
   *
   * @param exception    The exception that occurred.
   * @param errorMessage Formatted error message.
   * @param responseBody Raw response body if available.
   * @param httpStatus   HTTP status of the error.
   * @param httpRequest  The original HTTP request.
   * @param <RESPONSE>   Type of the response body.
   * @return ClientHttpResponse with error details.
   */
  private <REQUEST, RESPONSE> ClientHttpResponse<RESPONSE> handleExceptionInternal(
      final Exception exception,
      final String errorMessage,
      final String responseBody,
      final HttpStatus httpStatus,
      final ClientHttpRequest<REQUEST, RESPONSE> httpRequest) {
    log.error(
        "Exception while executing http request for requestUrl={}, status={}, errorMessage={}",
        httpRequest.getUrl(), httpStatus, errorMessage,
        exception); // Include 'exception' for stack trace
    httpRequest.getRetryHandlers()
        .forEach(handlerId -> RetryHandlerFactory.getHandler(handlerId.toString())
            .checkAndThrowRetriableException(exception));
    return ClientHttpResponse.<RESPONSE>builder().error(responseBody).status(httpStatus).build();
  }
}