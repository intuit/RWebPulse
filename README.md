[![Build Status](https://github.com/intuit/chain-z/actions/workflows/maven-build.yml/badge.svg)](https://github.com/intuit/chain-z/actions/workflows/maven-build.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.intuit.async/chain-z/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.intuit.async/chain-z)
![coverage](.github/badges/jacoco.svg)
![branches coverage](.github/badges/branches.svg)

# RWebPulse

RWebPulse is a ready to consume JAR library to easily integrate your springboot project with the latest reactive web-client offered by the spring. It is a one stop solution with config based initialisations, exception and retry handling.


## Who should use it?
Any springboot application which requires downstream communication with different servers through rest/ graphql apis can leverage RWebPulse.


## Why?
The existing webclient cannot be integrated in an easy configurable manner with an existing application. The app user needs to make significant changes in their code to be able to leverage the benefits of webclient. 

With RWebPulse we are providing seamless config based integration to WebClient with support of exception and retry handling. All the http parameters can be configured at the runtime by the config. The existing application needs to make minimal changes by just providing the right config and directly consume the webclient.


## How to integrate?


### Adding the config

Spring web client config needs to be defined like this in your application config.

```
spring-web-client-config:  
  connection-pool:         # Connection pool configuration
    pending-acquire-timeout: 31000 # 31 seconds
    max-idle-time: 31000 # 31 seconds
    max-life-time: 300000 # 5 minutes
    max-connections: 400 # max pool connections
  http-client-config:  # http client config
    connect-timeout-millis: 30000 # 30 seconds
    socket-timeout-millis: 30000 # 30 seconds
```



| Property | Description | Default values |
| ------ | ----------- | ------- |
| connection-pool |  |  |
| pending-acquire-timeout | Webclient fails if pool connection is pending for more than this duration | 31 seconds |
| max-idle-time | max time connection can remain idle before the server closes | 31 seconds |
| max-life-time | max life time of connection after which the server closes | 5 mins |
| max-connections | max connections that can be maintained in the pool | 400 |
| http-client-config |  |  |
| connect-timeout-millis | a time period in which a client should establish a connection with a server | 30 seconds |
| socket-timeout-millis | a maximum time of inactivity between two data packets when exchanging data with a server | 30 seconds |



### Adding the client

Add the below snippet in your application where you need to make a downstream service call

```
private final CommonSpringWebClient webClient;

protected ClientHttpResponse<Map> executeRequest(final Map<String, Object> body) {

    return webClient.syncHttpResponse(
            ClientHttpRequest.<Map, Map>builder()
                    .url("https:abc.com/v1/create")
                    .httpMethod(HttpMethod.POST)
                    .requestHeaders(new HttpHeaders())
                    .request(body)
                    .build());
  }
```


### Configure retries
[Retry Handling](./RetryHandling.md)


## [Contribution](./CONTRIBUTING.md)


## Local Development
[Local Development](./GETTING_STARTED.md)
