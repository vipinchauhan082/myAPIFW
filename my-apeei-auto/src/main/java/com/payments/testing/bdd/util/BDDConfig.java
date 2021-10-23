package com.payments.testing.bdd.util;

import com.github.dzieciou.testing.curl.CurlLoggingRestAssuredConfigFactory;
import com.github.dzieciou.testing.curl.Options;
import com.github.dzieciou.testing.curl.Platform;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.params.CoreConnectionPNames;
public class BDDConfig {

  private static final Config config = ConfigFactory.load();

  private static RestAssuredConfig restAssuredConfig =
      RestAssuredConfig.config().sslConfig(getSSLConfig()).httpClient(getHttpConfig()).redirect(new RedirectConfig());


  static {
    RestAssured.config =
        CurlLoggingRestAssuredConfigFactory.updateConfig(restAssuredConfig, getCurlOptions());
  }

  static {
    RestAssured.config = RestAssured.config().sslConfig(getSSLConfig().relaxedHTTPSValidation());
  }

//  static {
//    RestAssured.config =
//            RestAssured.config().httpClient(HttpClientConfig.httpClientConfig().setParam("CONNECTION_MANAGER_TIMEOUT", 5000));
//  }

  /**
   * typesafe config for module.
   *
   * @return initialized config for project
   */
  public static Config getConfig() {
    return config.getConfig("bddcore");
  }

  /**
   * config for rest assured.
   *
   * @return config for rest assured
   */
  public static RestAssuredConfig getRestAssuredConfig() {
    return restAssuredConfig;
  }

  /**
   * CURL logging configuration for RestAssured.
   *
   * @return configured options for curl logger
   */
  public static Options getCurlOptions() {
    return Options.builder()
        .updateCurl(curl -> curl.setInsecure(false))
        .printMultiliner()
        .useLongForm()
        .build();
  }

  /**
   * get the ssl configuration.
   *
   * @return the ssl configuration
   */
  public static SSLConfig getSSLConfig() {
    return new SSLConfig().allowAllHostnames().relaxedHTTPSValidation();
  }

  /**
   * http client configuration.
   *
   * @return the http config
   */
  public static HttpClientConfig getHttpConfig() {
    return HttpClientConfig.httpClientConfig()
    	.httpClientFactory(() -> new SystemDefaultHttpClient())
        /*.setParam(CoreConnectionPNames.CONNECTION_TIMEOUT,(long) getConfig().getInt("http.connection.requestTimeout"))
        .setParam(CoreConnectionPNames.SO_TIMEOUT, (long)getConfig().getInt("http.connection.socketTimeout"))
        .setParam(ClientPNames.CONN_MANAGER_TIMEOUT, (long)getConfig().getInt("http.connection.managerTimeout"))*/
            .setParam("http.connection.timeout", getConfig().getInt("http.connection.requestTimeout"))
            .setParam("http.socket.timeout", getConfig().getInt("http.connection.socketTimeout"))
            .setParam(
                    "http.connection-manager.timeout",
                    getConfig().getInt("http.connection.managerTimeout"));
    /*RequestConfig timeouts = RequestConfig.custom()
            .setConnectTimeout(getConfig().getInt("http.connection.managerTimeout"))
            .setConnectionRequestTimeout(getConfig().getInt("http.connection.requestTimeout"))
            .setSocketTimeout(getConfig().getInt("http.connection.socketTimeout"))
            .build();
    return HttpClientConfig.httpClientConfig()
            .httpClientFactory(HttpClientBuilder.create().setDefaultRequestConfig(timeouts).build());*/
  }
}
