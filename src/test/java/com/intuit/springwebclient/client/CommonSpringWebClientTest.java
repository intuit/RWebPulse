package com.intuit.springwebclient.client;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.UnknownContentTypeException;
import org.springframework.web.reactive.function.client.WebClient;

import com.intuit.springwebclient.entity.ClientHttpRequest;
import com.intuit.springwebclient.entity.ClientHttpRequest.ClientHttpRequestBuilder;

import reactor.core.publisher.Mono;


@ExtendWith(MockitoExtension.class)
public class CommonSpringWebClientTest {

    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    WebClient.RequestHeadersSpec headersSpec;
    @Mock
    WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private CommonSpringWebClient commonSpringWebClient;



    @Test
    public void testSyncHttpResponseSuccess() {
        ClientHttpRequest clientHttpRequest = createClientHttpRequest().build();
        mockRequestBody();

        Mockito.when(headersSpec.retrieve()).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(ParameterizedTypeReference.forType(String.class))).thenReturn(Mono.just("ztest"));


        commonSpringWebClient.syncHttpResponse(clientHttpRequest);
    }

    @Test
    public void testSyncHttpResponseSuccessNoRequestBody() {
        ClientHttpRequest clientHttpRequest = createClientHttpRequest().request(null).requestType(null).build();
        Mockito.when(webClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
        Mockito.doReturn(requestBodyUriSpec).when(requestBodyUriSpec).uri("test-url");
        Mockito.when(requestBodyUriSpec.headers(Mockito.any())).thenReturn(requestBodySpec);

        commonSpringWebClient.syncHttpResponse(clientHttpRequest);
    }

    @Test
    public void testHttpStatusCodeException() {

        ClientHttpRequest clientHttpRequest = createClientHttpRequest().build();
        mockRequestBody();

        HttpClientErrorException httpClientErrorException = Mockito.mock(HttpClientErrorException.class);
        Mockito.when(httpClientErrorException.getResponseBodyAsString()).thenReturn("Page not found");
        Mockito.when(httpClientErrorException.getResponseHeaders()).thenReturn(new HttpHeaders());
        Mockito.when(httpClientErrorException.getStatusCode()).thenReturn(HttpStatus.valueOf(404));

        Mockito.when(headersSpec.retrieve()).thenThrow(httpClientErrorException);

        commonSpringWebClient.syncHttpResponse(clientHttpRequest);
    }

    @Test
    public void testUnknownContentTypeException() {
        ClientHttpRequest clientHttpRequest = createClientHttpRequest().build();
        mockRequestBody();

        UnknownContentTypeException unknownContentTypeException = Mockito.mock(UnknownContentTypeException.class);
        Mockito.when(unknownContentTypeException.getResponseBodyAsString()).thenReturn("Page not found");
        Mockito.when(unknownContentTypeException.getResponseHeaders()).thenReturn(new HttpHeaders());
        Mockito.when(unknownContentTypeException.getRawStatusCode()).thenReturn(415);

        Mockito.when(headersSpec.retrieve()).thenThrow(unknownContentTypeException);

        commonSpringWebClient.syncHttpResponse(clientHttpRequest);
    }

    @Test
    public void testOtherException() {
        ClientHttpRequest clientHttpRequest = createClientHttpRequest().build();
        mockRequestBody();

        Mockito.when(headersSpec.retrieve()).thenThrow(IllegalArgumentException.class);

        commonSpringWebClient.syncHttpResponse(clientHttpRequest);
    }

    private ClientHttpRequestBuilder<Object, Object> createClientHttpRequest() {
        HttpHeaders httpHeadersMock = new HttpHeaders();
        Consumer<HttpHeaders> httpHeadersConsumer = new Consumer<HttpHeaders>() {
            @Override
            public void accept(HttpHeaders httpHeaders) {
                return;
            }
        };
        httpHeadersConsumer.accept(httpHeadersMock);

        return ClientHttpRequest.builder()
            .httpMethod(HttpMethod.GET)
            .url("test-url")
            .requestHeaders(httpHeadersMock)
            .requestType(ParameterizedTypeReference.forType(String.class))
            .request("hello")
            .responseType(ParameterizedTypeReference.forType(String.class));
    }

    private void mockRequestBody() {
        Mockito.when(webClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
        Mockito.doReturn(requestBodyUriSpec).when(requestBodyUriSpec).uri("test-url");
        Mockito.when(requestBodyUriSpec.headers(Mockito.any())).thenReturn(requestBodySpec);
        Mockito.when(requestBodySpec.body("hello", ParameterizedTypeReference.forType(String.class))).thenReturn(headersSpec);
    }
}
