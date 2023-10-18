package com.intuit.springwebclient.client;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.UnknownContentTypeException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.intuit.springwebclient.entity.ClientHttpRequest;
import com.intuit.springwebclient.entity.ClientHttpResponse;
import com.intuit.springwebclient.retryHandler.RetryHandlerFactory;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Spring 5 Web Client Method executor.
 */
@Slf4j
@Component
public class CommonSpringWebClient {
    private final WebClient webClient;

	public CommonSpringWebClient(@Qualifier("RWebPulseClient") WebClient webClient) {
		this.webClient = webClient;
	}

	/**
     * Execute Blocking http request.
     * @param httpRequest
     * @return
     * @param <REQUEST>
     * @param <RESPONSE>
     */
    public <REQUEST, RESPONSE> ClientHttpResponse<RESPONSE> syncHttpResponse(ClientHttpRequest<REQUEST, RESPONSE> httpRequest) {
		try {
			log.info("Executing http request for request={}, method={}", httpRequest.getRequest(),
					httpRequest.getHttpMethod());
			return generateResponseSpec(httpRequest).toEntity(httpRequest.getResponseType())
					.map(this::generateResponse).retryWhen(generateRetrySpec(httpRequest)).block();
		} catch (final WebClientResponseException ex) {
			final String errorMessage = String.format("Error in making rest call. Error=%s Headers=%s statusCode=%s",
					ex.getResponseBodyAsString(), ex.getHeaders(), ex.getStatusCode());
			return handleException(ex, errorMessage, HttpStatus.valueOf(ex.getStatusCode().value()), httpRequest);
		} catch (final HttpStatusCodeException ex) {
			final String errorMessage = String.format("Error in making rest call. Error=%s Headers=%s statusCode=%s",
					ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode());
			return handleException(ex, errorMessage, HttpStatus.valueOf(ex.getStatusCode().value()), httpRequest);
		} catch (final UnknownContentTypeException ex) {
			// It was observed that this exception was thrown whenever there was a HTTP 5XX error
			// returned in the REST call. The handle went into `RestClientException` which is the parent
			// class of `UnknownContentTypeException` and hence some contextual information was lost
			final String errorMessage = String.format("Error in making rest call. Error=%s Headers=%s",
					ex.getResponseBodyAsString(), ex.getResponseHeaders());
			return handleException(ex, errorMessage, HttpStatus.valueOf(ex.getRawStatusCode()), httpRequest);
		} catch (final Exception ex) {
			final String errorMessage = String
					.format("Error in making rest call. Error=%s", ex.getMessage());
			return handleException(ex, errorMessage, HttpStatus.INTERNAL_SERVER_ERROR, httpRequest);
		}
	}

    /**
	 * Generate Web Client Response spec from http request.
	 * 
	 * @param httpRequest
	 * @return
	 */
	private <REQUEST, RESPONSE> WebClient.ResponseSpec generateResponseSpec(
			ClientHttpRequest<REQUEST, RESPONSE> httpRequest) {

		Consumer<HttpHeaders> httpHeadersConsumer = (httpHeaders -> httpHeaders
				.putAll(httpRequest.getRequestHeaders()));
		RequestBodySpec webClientBuilder = webClient.method(httpRequest.getHttpMethod()).uri(httpRequest.getUrl())
				.headers(httpHeadersConsumer);

		// set only when provided
		if (Objects.nonNull(httpRequest.getRequest()) && Objects.nonNull(httpRequest.getRequestType())) {
			webClientBuilder.body(Mono.just(httpRequest.getRequest()), httpRequest.getRequestType());
		}

		return webClientBuilder.retrieve();

	}

    /**
     * Generates retry spec for the request based on config provided.
     * @param httpRequest
     * @return
     */
	private <REQUEST, RESPONSE> Retry generateRetrySpec(ClientHttpRequest<REQUEST, RESPONSE> httpRequest) {
		return Retry
				.fixedDelay(httpRequest.getClientRetryConfig().getMaxAttempts(),
						Duration.ofSeconds(httpRequest.getClientRetryConfig().getBackOff()))
				.doBeforeRetry(signal -> log.info("Retrying for requestUrl={}, retryCount={}", httpRequest.getUrl(),
						signal.totalRetries()))
				.filter(httpRequest.getClientRetryConfig().getRetryFilter());
	}

	/**
	 * Handle Success response.
	 * 
	 * @param response
	 * @return
	 * @param <RESPONSE>
	 */
	private <RESPONSE> ClientHttpResponse<RESPONSE> generateResponse(ResponseEntity<RESPONSE> response) {
		return ClientHttpResponse.<RESPONSE>builder().response(response.getBody()).status(response.getStatusCode())
				.isSuccess2xx(response.getStatusCode().is2xxSuccessful()).build();
	}

    /**
     * Handle Exception and send back response.
     * @param exception
     * @param errorMessage
     * @param httpStatus
     * @param httpRequest
     * @return
     * @param <RESPONSE>
     */
    private <REQUEST, RESPONSE> ClientHttpResponse<RESPONSE> handleException(
            final Exception exception,
            final String errorMessage,
            final HttpStatus httpStatus,
            final ClientHttpRequest<REQUEST, RESPONSE> httpRequest) {
        log.error("Exception while executing http request for requestUrl={}, status={}, errorMessage={}", httpRequest.getUrl(), httpStatus, errorMessage);
        httpRequest.getRetryHandlers()
                .forEach(handlerId -> RetryHandlerFactory.getHandler(handlerId.toString()).checkAndThrowRetriableException(exception));
        return ClientHttpResponse.<RESPONSE>builder().error(errorMessage).status(httpStatus).build();
    }
}
