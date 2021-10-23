package com.payments.testing.bdd.http;

import com.payments.testing.bdd.util.BDDConfig;
import com.payments.testing.bdd.util.YamlReader;
import com.typesafe.config.Config;
import io.restassured.http.Method;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

public abstract class RequestState {

    private static final Logger LOG = LoggerFactory.getLogger(RequestState.class);
    private static Config config = BDDConfig.getConfig();
    private static Pattern QS_PATTERN = Pattern.compile("(\\w+)=?([^&]+)?");
    private Map<String, List<String>> params = new HashMap<>();
    private Map<String, String> pathParams = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> formParams = new HashMap<>();
    private Map<String, String> cookies = new HashMap<>();
    private String body;
    private Method httpMethod;
    private String uri;
    private String host;
    private String proxyHost;
    private int proxyPort;
    private String proxyScheme;

    /**
     * get response object; will execute request. if it hasn't already been executed or it's been
     * reset
     *
     * @return a responsestate to play with
     */
    public abstract ResponseState getResponseState();

    /**
     * resets the state of this object.
     */
    public void reset() {
        resetBasicExceptHeaders();
        setHost(config.getString("request.server.host"));
//        setProxy("", 0);
    }

    public void reset(String hostName) {
        resetBasicExceptHeaders();
        setHost(config.getString("request.server." + hostName));
    }

    public void resetBasicExceptHeaders() {
        params.clear();
        pathParams.clear();
        formParams.clear();
        cookies.clear();
        resetHeaders();
        body = null;
        httpMethod = null;
        uri = null;
        try {
            setProxy(config.getString("request.server.proxy.host"), config.getInt("request.server.proxy.port"));
        } catch (Exception ignore) {
            setProxy("", 0);
        }
        try {
            setProxyScheme(config.getString("request.server.proxy.scheme"));
        } catch (Exception ignore) {
            setProxyScheme("http");
        }
    }

    /**
     * get the proxy host for this request.
     *
     * @return the proxy host.
     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * get the proxy port for this request.
     *
     * @return the proxy port.
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * get the proxy scheme for this request.
     *
     * @return the proxy scheme.
     */
    public String getProxyScheme() {
        return proxyScheme;
    }

    /**
     * @param host
     * @param port
     * @param scheme
     */
    public void setProxy(String host, int port, String scheme) {
        this.proxyHost = host;
        this.proxyPort = port;
        this.proxyScheme = scheme;
    }

    /**
     * @param host
     * @param port
     */
    public void setProxy(String host, int port) {
        this.proxyHost = host;
        this.proxyPort = port;
        this.proxyScheme = "http";
    }

    /**
     * @param scheme
     */
    public void setProxyScheme(String scheme) {
        this.proxyScheme = scheme;
    }

    /**
     * resets the headers by clearing them and then loading the default headers from the
     * configuration.
     */
    public void resetHeaders() {
        headers.clear();
        config
                .getObjectList("request.defaults.headers")
                .forEach(
                        obj -> {
                            Map<String, Object> header = obj.unwrapped();
                            header.forEach((k, v) -> setHeader(k, v.toString()));
                        });
    }

    /**
     * get the HTTP method for this request.
     *
     * @return the http method.
     */
    public Method getHttpMethod() {
        return httpMethod;
    }

    /**
     * set method for the request.
     *
     * @param method the request method
     */
    public void setHttpMethod(String method) {
        try {
            httpMethod = Method.valueOf(method);
        } catch (IllegalArgumentException iae) {
            LOG.error("Not a valid httpmethod {}", method);
        } catch (NullPointerException npe) {
            LOG.error("method was null");
        }
    }

    /**
     * get the URI for the request.
     *
     * @return the URI for the request
     */
    public String getURI() {
        return uri;
    }

    /**
     * set the URI for the request.
     *
     * @param uri the uri for the request.
     */
    public void setURI(String uri) {
        setParamsFromURI(uri);
        if (Objects.nonNull(uri) && uri.contains("?")) {
            uri = uri.substring(0, uri.indexOf('?'));
        }
        this.uri = uri;
    }

    /**
     * get the URI for the request.
     *
     * @return the URI for the request
     */
    public String getHost() {
        return host;
    }

    /**
     * set the URI for the request.
     *
     * @param host the uri for the request.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * get the request headers.
     *
     * @return the request headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Set map headers for the request.
     *
     * @param header reset the headers to this map
     */
    public void setHeaders(Map<String, String> header) {
        headers.putAll(header);
    }

    /**
     * add header name/value to the request.
     *
     * @param name  the header name
     * @param value the header value
     */
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    /**
     * remove header name to the request.
     *
     * @param name  the header name

     */
    public void removeHeader(String name) {
        headers.remove(name);
    }
    public void setCookies(Map<String, String> cookiesMap) {
        cookies.putAll(cookiesMap);
    }

    public Map<String, String> getCookies() {
        return cookies;
    }
    /**
     * Get request paramters.
     *
     * @return the request parameters
     */
    public Map<String, List<String>> getParameters() {
        return params;
    }

    public Map<String, String> getPathParameters() {
        return pathParams;
    }

    public Map<String, String> getFormParameters() {
        return formParams;
    }

    /**
     * get named parameter value.
     *
     * @param name the name of the parameter
     * @return the parameter value
     */
    public List<String> getParameter(String name) {
        return params.get(name);
    }

    /**
     * sets the parameters from a URI.
     *
     * @param uri the uri to parse
     */
    private void setParamsFromURI(String uri) {

        if (Objects.isNull(uri) || !uri.contains("?")) {
            return;
        }

        Matcher matcher = QS_PATTERN.matcher(uri.substring(uri.indexOf('?') + 1));
        while (matcher.find()) {
            setParameter(matcher.group(1), matcher.group(2));
        }
    }

    /**
     * add a named parameter (either qs or form depending on request type).
     *
     * @param name the parameter name
     * @param val  the parameter value
     */
    public void setParameter(String name, String val) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        params.computeIfAbsent(name, list -> new ArrayList<>()).add(val);
    }

    public void setPathParameter(String name, String val) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        pathParams.put(name, val);
    }

    public void setFormParameter(String name, String val) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        formParams.put(name, val);
    }

    /**
     * get the request body.
     *
     * @return the request body
     */
    public String getBody() {
        return body;
    }

    /**
     * set the request body.
     *
     * @param body the body as text
     */
    public void setBody(String body) {
        this.body = body;
    }

    public String setSchemaWithTestData(String requestEntity, String schema, String testData) throws IOException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        YamlReader yamlReader = new YamlReader(ClassLoader.getSystemResourceAsStream(config
                .getConfig("files").getString("testdata").replace("{testdata_name}", schema)));
        Map<String, Object> selectData = yamlReader.getYamlObj(testData);
        
        if(selectData.containsKey("CustomerId") && new File(System.getProperty("user.dir") + File.separator + "testdata.json").exists()){
                String testDataValue = System.getProperty("customerId");
                if (testDataValue != null)
                    selectData.put("CustomerId", testDataValue);
        }
        return YamlReader.replaceJsonWithYmlDataAsString(schema, requestEntity, selectData).replaceAll("\\{TimeStamp}", String.valueOf(timestamp.getTime()))
                .replaceAll("\\{RandomUUID}", UUID.randomUUID().toString()).replaceAll("\\{random}", RandomStringUtils.randomAlphabetic(6))
                .replaceAll("\\{RandomUUID1}", UUID.randomUUID().toString()).replaceAll("\\{random}", RandomStringUtils.randomAlphabetic(6))
                .replaceAll("\\{RandomUUID2}", UUID.randomUUID().toString()).replaceAll("\\{random}", RandomStringUtils.randomAlphabetic(6));
    }

    public String setSchemaWithSOAPTestData(String requestEntity, String schema, String testData) throws IOException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        YamlReader yamlReader = new YamlReader(ClassLoader.getSystemResourceAsStream(config
                .getConfig("files").getString("testdata").replace("{testdata_name}", schema)));
        Map<String, Object> selectData = yamlReader.getYamlObj(testData);
        return selectData.get("soap_request_body").toString();
    }

    public Map<String, String> setSchemaWithTestDataAsMap(String requestEntity, String schema, String testData) throws IOException {
        YamlReader yamlReader = new YamlReader(ClassLoader.getSystemResourceAsStream(config
                .getConfig("files").getString("testdata").replace("{testdata_name}", schema)));
        Map<String, Object> selectData = yamlReader.getYamlObj(testData);

        return yamlReader.replaceJsonWithYmlDataAsMap(schema, requestEntity, selectData);
    }

}
