package com.payments.testing.bdd.api.http;

import com.github.dzieciou.testing.curl.CurlLoggingRestAssuredConfigFactory;
import com.payments.testing.bdd.http.RequestState;
import com.payments.testing.bdd.http.ResponseState;
import com.payments.testing.bdd.util.BDDConfig;
import com.payments.testing.bdd.util.HttpTrustManager;
import com.typesafe.config.Config;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.Random;
import java.util.UUID;

import io.restassured.config.LogConfig;


public class APIRequestState extends RequestState {

    private static Config config = BDDConfig.getConfig();
    private static final Logger LOG = LoggerFactory.getLogger(APIRequestState.class);
    private ResponseState responseState = null;
    private APIResponseStateType responseStateType;

    /**
     * simple constructor for injection.
     */
    public APIRequestState() {
    }

    /**
     * helper to handle process of response body.
     *
     * @param type an API
     */
    public void setResponseStateType(APIResponseStateType type) {
        responseStateType = type;
    }

    /**
     * what type of processor should this use.
     *
     * @return the type processor to use
     */
    public APIResponseStateType getResponseStateType() {
        return responseStateType;
    }

    @Override
    public void reset() {
        responseState = null;
        super.reset();
    }

    public void reset(String hostName) {
        responseState = null;
        super.reset(hostName);
    }

    /**
     * get the response. NOTE: if not explicitly set, restassured sets the charset and content-type
     * headers
     *
     * @return response object to play with
     */
    @Override
    public ResponseState getResponseState() {

        if (responseState != null) {
            return responseState;
        }

        // set up the request
        final RequestSpecification request = RestAssured.given().baseUri(getHost()).basePath(getURI()).log().all();
        //temporary relaxed https validation
        request.relaxedHTTPSValidation();
        HttpTrustManager.trustAllHttpsCertificates();
        // set up test headers
        getHeaders()
                .forEach(
                        (el1, el2) -> {
                            request.header(el1, el2);
                        });
        if (!getPathParameters().isEmpty()) {
            request.pathParams(getPathParameters());
        }

        if (!getFormParameters().isEmpty()) {
            request.formParams(getFormParameters());
        }
        // parameters
        getParameters()
                .forEach(
                        request::queryParam);
        //set the proxy if needed
        if (!getProxyHost().equals(""))
            request.proxy(getProxyHost(), getProxyPort(), getProxyScheme());
        // set the body (if needed)
        if (getBody() != null) {
            request.body(getBody());
        }
        // perform the request
        responseState = new APIResponseState(request.request(getHttpMethod()), getResponseStateType());

        // temporary hack to close response
        responseState.getResponse().body().asString();

        return responseState;
    }

    public ResponseState getResponseState(Object stream) {

        if (responseState != null) {
            return responseState;
        }
        PrintStream fileLog = (PrintStream) stream;
        //RestAssured.config = RestAssured.config().logConfig(LogConfig.logConfig().defaultStream(fileLog).enablePrettyPrinting(true));
        // set up the request
        RequestSpecification request = RestAssured.given().config(CurlLoggingRestAssuredConfigFactory.updateConfig(BDDConfig.getRestAssuredConfig().logConfig(LogConfig.logConfig().defaultStream(fileLog).enablePrettyPrinting(true)), BDDConfig.getCurlOptions())).baseUri(getHost()).basePath(getURI()).log().all();
        //temporary relaxed https validation
        request.relaxedHTTPSValidation();
        HttpTrustManager.trustAllHttpsCertificates();
        //set correlationid based on flag

        if(config.getBoolean("flags.randomCorrelation"))
        {
            setHeader("x-lbg-txn-correlation-id", UUID.randomUUID().toString());
        }

        // set up test headers
        getHeaders()
                .forEach(
                        (el1, el2) -> {
                            request.header(el1, el2);
                        });
        if (!getPathParameters().isEmpty()) {
            request.pathParams(getPathParameters());
        }

        if (!getFormParameters().isEmpty()) {
            request.formParams(getFormParameters());
        }
        // parameters
        getParameters()
                .forEach(
                        request::queryParam);
        //set the proxy if needed
        if (!getProxyHost().equals(""))
            request.proxy(getProxyHost(), getProxyPort(), getProxyScheme());
        // set the body (if needed)
        if (getBody() != null) {
            request.body(getBody());
        }
        if(!(getCookies().isEmpty())) {
            request.cookies(getCookies());
        }
        // perform the request
        responseState = new APIResponseState(request.redirects().follow(false).request(getHttpMethod()), getResponseStateType());

        // temporary hack to close response
        responseState.getResponse().body().prettyPrint();

        return responseState;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
