# Retry Handling

This library provides retry capabilites based on the runtime attributes as well as provides custom retry handling based on the response.

Retry Config can be passed along with [ClientHttpRequest](./src/main/java/com/intuit/springwebclient/entity/ClientHttpRequest.java)

```
  webClient.syncHttpResponse(
            ClientHttpRequest.<Map, Map>builder()
                    .url("https:abc.com/v1/create")
                    .httpMethod(HttpMethod.POST)
                    .requestHeaders(new HttpHeaders())
                    .request(body)
                    .clientRetryConfig()
                    .build())
```

### Client Retry Config

[WebClientRetryConfig](./src/main/java/com/intuit/springwebclient/config/WebClientRetryConfig.java)

| Attribute | Description | Default |
| -------- | --------------------- | ---- |
| maxAttempts | Maximum number of reties | 0 |
| backOff | Backoff time between retries in seconds | 0 |

### Custom Retry Handlers

Custom retry handlers can be added, which would get invoked after exhaution of all reties specified in Client Retry Config.

#### Steps

1. Implement custom retry handler. [RetryHandler](./src/main/java/com/intuit/springwebclient/retryHandler/RetryHandler.java)
2. Populate the Retry handler factory at the application start event.
```
@Component
@AllArgsConstructor
public class ApplicationEventListener {

  private final List<RetryHandler> RetryHandler;
  @EventListener
  public void handleContextRefresh(ContextRefreshedEvent event) {

    // Init Retry Handler Factory
    RetryHandler.forEach(
            handler -> RetryHandlerFactory.addHandler(handler.getName(), handler));
  }
}
```
3. Pass the list fo handlers to be called in the ClientHttpRequest.

```
  webClient.syncHttpResponse(
            ClientHttpRequest.<Map, Map>builder()
                    .url("https:abc.com/v1/create")
                    .httpMethod(HttpMethod.POST)
                    .requestHeaders(new HttpHeaders())
                    .request(body)
                    .retryHandlers()
                    .build())
```
