package com.payments.testing.bdd.api.steps;

import com.payments.testing.bdd.api.http.APIRequestState;
import com.payments.testing.bdd.api.http.APIResponseStateType;
import com.payments.testing.bdd.parameters.DefaultParamTransformer;
import com.payments.testing.bdd.util.BDDConfig;
import com.payments.testing.bdd.util.Helper;
import com.payments.testing.bdd.util.YamlReader;
import com.typesafe.config.Config;
import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java8.En;
import io.cucumber.datatable.DataTable;
import io.cucumber.datatable.dependency.com.fasterxml.jackson.core.type.TypeReference;
import io.cucumber.datatable.dependency.com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.dependency.com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.path.json.JsonPath;
import javafx.util.Pair;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.payments.testing.bdd.util.Helper.getCurrentDateTime;
import static org.junit.Assert.assertTrue;

public class BasicSteps implements En {

    private static Config config = BDDConfig.getConfig();
    private static final Logger CURL_LOG = LoggerFactory.getLogger("CURL");
    private String logFolder = config.getString("files.restLogDir");

    /**
     * container for cucumber lambda methods.
     *
     * @param requestState     injected object
     * @param paramTransformer injected object
     */
    public BasicSteps(APIRequestState requestState, DefaultParamTransformer paramTransformer) {
        Helper helper = new Helper(requestState, paramTransformer);
        /** reset the request state */
        Before(
                (Scenario scenario) -> {
                    ThreadLocal<PrintStream> localStream = new ThreadLocal<PrintStream>();
                    //PrintStream printStream = null;
                    //LogConfig originalLogConfig = RestAssured.config().getLogConfig();
                    File fileWriter;
                    String scenarioID = scenario.getId();
                    String[] parts = scenarioID.split("[.;:/]");
                    String dir = logFolder + "/" + parts[0] + "/" + parts[1] + "/" + parts[2];
                    File featureDirectory = new File(dir);
                    if (!featureDirectory.exists())
                        featureDirectory.mkdirs();
                    fileWriter = new File(dir + "/" + parts[2] + parts[4] + ".log");
                    if (!fileWriter.exists()) {
                        try {
                            fileWriter.createNewFile();
                        } catch (IOException e) {
                            System.out.println("file not created:" + fileWriter.getPath());
                        }
                    }
                    try {
                        localStream.set(new PrintStream(new FileOutputStream(fileWriter), true));
                        //printStream = new PrintStream(new FileOutputStream(fileWriter), true);
                        //printStream = localStream.get();
                        paramTransformer.cacheAnObject("log", localStream.get());
                        //RestAssured.config = BDDConfig.getRestAssuredConfig().logConfig(LogConfig.logConfig().defaultStream(localStream.get()).enablePrettyPrinting(true));
                        //RestAssured.config = RestAssured.config().logConfig(LogConfig.logConfig().defaultStream(localStream.get()).enablePrettyPrinting(true));
                    } catch (FileNotFoundException e) {
                        System.out.println("file not found");
                    }
                    MDC.put("curlLogDir", dir);
                    CURL_LOG.debug("## SCENARIO: {}", scenario.getName());
                });

        /**
         * @desc Initiates the request and the right parser - either JSON, XML or HTML
         */
        Given(
                "^I am a (JSON|XML|HTML) API consumer$",
                (String type) -> {
                    helper.initiateRequestAndSetParser(type);
                });

        Given("^I generate auth token$",
                () -> {
                    helper.generateAuthTokan();

                });

        Given("^I validate auth token$",
                () -> {
                    helper.validateAuthTokan();

                });
        /**
         * @desc Initiates the request and set parser using consumer configuration
         */
        Given(
                "I'm a {string}",
                (String consumer) -> {
                    requestState.reset();
                    String type = config.getConfig("consumers").getString(consumer);
                    requestState.setResponseStateType(APIResponseStateType.valueOf(type));
                });

        /**
         * @desc Sets the correlation-id header (default named X-Correlation-ID, but can be
         * overridden in config)
         * @param id the X-Correlation-ID header value
         */
        When(
                "I am executing test {string}",
                (String id) -> {
                    requestState.setHeader(config.getString("request.correlationIdName"), id);
                });

        /**
         * set the request timeout.
         *
         * @param timeout the timeout in millis
         */
        When(
                "I request a maximum response time of {long}",
                (Long timeout) -> {
                    long actual = requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().getTime();
                    assertTrue(
                            String.format(
                                    "Request expected to return in %d or less, but returned in %d", timeout, actual),
                            timeout >= actual);
                });

        /**
         * set the HTTP method and URI for the request
         *
         * @param method the type of request (e.g. httpmethod)
         * @param uri the uri to execute against
         */
        When(
                "^I request (GET|POST|DELETE|PATCH|PUT) \"([^\"]*)\"$",
                helper::setURIAndMethod);

        /**
         * set the HTTP method, URI and Host for the request
         *
         * @param method the type of request (e.g. httpmethod)
         * @param uri the uri to execute against
         * @param host the host of the URL
         */
        When(
                "^I request (GET|POST|DELETE|PATCH|PUT) \"([^\"]*)\" on \"([^\"]*)\"$",
                helper::setUriMethodAndHost);

        /**
         * sets the request body json or xml.
         *
         * @param type the type of content (JSON or XML)
         * @param body the actual json or xml
         */
        When(
                "^I set the (JSON|XML) body to",
                (String type, String body) -> {
                    helper.setRequestBodyAndContentType(type, body);
                });

        /**
         * sets the body from data table
         *
         * @param type the type of content (JSON or XML)
         * @param table the actual json or xml as dot notation
         */
        When("^I set the (JSON|XML) body from values",
                (String type, DataTable dataTable) -> {
                    helper.setRequestBodyAndContentType(type, dataTable);
                });

        /**
         * sets the request body json or xml.
         *
         * @param action the SOAPAction
         * @param body the XML
         */
        When(
                "I set the SOAPAction to {string} and body as",
                (String action, String body) -> {
                    requestState.setHeader("SOAPAction", paramTransformer.transform(action));
                    helper.setRequestBodyAndContentType("XML", body);
                });

        /**
         * pause the execution before next step
         *
         * @param time the time to pause
         * @param timeunit the unit of measurement
         */
        When(
                "I wait {long} {string}",
                (Long time, String timeunit) -> {
                    try {
                        TimeUnit tu = TimeUnit.valueOf(timeunit.toUpperCase(Locale.US));
                        tu.sleep(time);
                    } catch (Exception e) {
                        throw new Exception("Invalid timeunit type " + timeunit);
                    }
                });

//        /**
//         * Read request body from YML file
//         *
//         * @param type the type of contect (JSON/XML)
//         * @param object the YML file name
//         *
//         */
//        Given(
//                "I set the {string} body from file object {string} in {string}",
//                (String type, String object, String location) -> {
//                    YamlReader yaml = new YamlReader(ClassLoader.getSystemResourceAsStream(config.getConfig("files").getString(location)));
//                    Map<String, Object> yamlMap = yaml.getYamlObj(object);
//                    String body = "";
//                    ObjectMapper mapper = new ObjectMapper();
//                    body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(yamlMap);
//                    requestState.setHeader("Content-Type", ContentType.valueOf(type).withCharset("utf-8"));
//                    requestState.setBody(paramTransformer.transform(body));
//                }
//        );

        Given("I set the request body for {string} from {string}",
                (String schema, String testData) -> {
                    requestState.setBody(helper.setJsonWithYmlData("request", schema, testData));
                });

        Given("I set the request body for {string} from {string} yml",
                (String ymlFile, String testData) -> {
                    requestState.setBody(helper.readYmlObjectToString(ymlFile, testData));
                });

        Given("I set the request body with {string} as response value {string}",
                (String value, String responseKey) -> {
                    String paramVal = paramTransformer.transform(responseKey);
                    helper.replaceReqBodyFromSystemPropCustId(paramVal,value);
                });

        Given("I set the request body with {string} as value {string}",
                (String keyValue, String actualValue) -> {
                    requestState.setBody(requestState.getBody().replaceAll("\\{" + keyValue + "}", actualValue));
                });

        Given("^I set the \"([^\"]*)\" in the request body for \"([^\"]*)\" from \"([^\"]*)\"$",
                (String param, String ymlFile, String testData) -> {
                    helper.replaceValueInReqBody(param, ymlFile, testData);
                });

        When("^I request (GET|POST|DELETE|PATCH|PUT) for \"([^\"]*)\"$",
                (String method, String schema) -> {
                    helper.setHttpMethodAndUriFromJson("/baseuri/basepath", method, schema);
                });

        When("^I request (GET|POST|DELETE|PATCH|PUT) for \"([^\"]*)\" from \"([^\"]*)\" testdata$",
                (String method, String endPointName, String schema) -> {
                    requestState.setHttpMethod(method);
                    Pair<ObjectNode, ObjectMapper> readerObjectMapperPair = YamlReader.readJsonSchemaFile(schema);
                    requestState.setURI(YamlReader
                            .getJsonNodeValue(readerObjectMapperPair.getKey(), "/" + endPointName + "/basepath"));
                });


        When("^I request (GET|POST|DELETE|PATCH|PUT) for \"([^\"]*)\" from \"([^\"]*)\"$",
                (String method, String schema, String testData) -> {
                    helper.setHttpMethodAndUriFromYmlFile("baseuri", "basepath", method, schema, testData);
                });

        Given("^I am checking \"([^\"]*)\" for (JSON|XML|HTML) API consumer$",
                (String hostName, String type) -> {
                    helper.initiateRequestAndSetParser(hostName, type);
                });

//        Given("^I create account \"([^\"]*)\" from \"([^\"]*)\" using auth token \"([^\"]*)\"$",
//                (String schema, String testData, String authToken) -> {
//                    helper.initiateRequestAndSetParser();
//                    helper.createAccount(schema, authToken, testData);
//                });
//
//        Given("^I create account using \"([^\"]*)\" schema for \"([^\"]*)\" with auth token \"([^\"]*)\"$",
//                (String schema, String testData, String authToken) -> {
//                    helper.initiateRequestAndSetDefaultParser("istio");
//                    helper.createAccount(schema, authToken, testData);
//                });
//
//        Given("^I update account \"([^\"]*)\" from \"([^\"]*)\" using auth token \"([^\"]*)\"$",
//                (String accountId, String testData, String authToken) -> {
//                    helper.iUpdateAccount(accountId, testData, authToken);
//                });

//        Given("^I retrieve the account \"([^\"]*)\" using auth token \"([^\"]*)\"$",
//                (String accountId, String authToken) -> {
//                    String accountIdNew = paramTransformer.transform(accountId);
//                    String schema = "getAccount";
//                    requestState.reset();
//                    requestState.setResponseStateType(APIResponseStateType.valueOf("JSON"));
//                    Pair<ObjectNode, ObjectMapper> readerObjectMapperPair = YamlReader.readJsonSchemaFile(schema);
//                    Map<String, String> headers = readerObjectMapperPair.getValue().readValue(String.valueOf(readerObjectMapperPair.getKey().get("headers")),
//                            new TypeReference<Map<String, String>>() {
//                            });
//                    if (config.getBoolean("flags.staticAuth"))
//                        headers.put("Authorization", "Bearer VALID_STATIC_TOKEN");
//                    else
//                        headers.put("Authorization", paramTransformer.transform(authToken));
//                    headers.forEach(requestState::setHeader);
//                    requestState.setPathParameter("accountid", accountIdNew);
//                    requestState.setHttpMethod("GET");
//                    String uri = new ObjectMapper().readValue(requestState
//                            .setSchemaWithTestData("baseuri", schema, "Account1-baseuri"), ObjectNode.class)
//                            .get("basepath").textValue();
//                    requestState.setURI(uri);
//                    requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//                });
//
//        Given("^I check \"([^\"]*)\" to generate \"([^\"]*)\" from \"([^\"]*)\"$",
//                (String hostName, String schema, String testData) -> {
//                    if (!config.getBoolean("flags.staticAuth")) {
//                        requestState.reset(hostName);
//                        requestState.setResponseStateType(APIResponseStateType.valueOf("JSON"));
//                        Map<String, String> headers = requestState.setSchemaWithTestDataAsMap("headers", schema, testData + "-headers");
//                        headers.forEach(requestState::setHeader);
//                        YamlReader yamlReader = new YamlReader(ClassLoader.getSystemResourceAsStream(config
//                                .getConfig("files").getString("testdata").replace("{testdata_name}", schema)));
//                        Map<String, Object> formParam = yamlReader.getYamlObj(testData + "-formparams");
//                        Map<String, String> newFormParams = formParam.entrySet().stream()
//                                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
//                        for (Map.Entry<String, String> entry : newFormParams.entrySet())
//                            requestState.setFormParameter(entry.getKey(), entry.getValue());
//                        requestState.setHttpMethod("POST");
//                        Pair<ObjectNode, ObjectMapper> readerObjectMapperPair = YamlReader.readJsonSchemaFile(schema);
//                        requestState.setURI(YamlReader
//                                .getJsonNodeValue(readerObjectMapperPair.getKey(), "/baseuri/basepath"));
//                        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//                    }
//                });
        Then("^record the value \"([^\"]*)\" to a file \"([^\"]*)\"$",
                (String tag, String fileName) -> {
                    if (!(config.getBoolean("flags.staticAuth"))) {
                        String pathValue = requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().jsonPath().get(tag).toString();
                        BufferedWriter out = new BufferedWriter(
                                new FileWriter(fileName, true));
                        out.write(getCurrentDateTime("yyyy/MM/dd HH:mm:ss") + ": " + pathValue + "\n");
                        out.close();
                    }});

//        When("^I get account id from response \"([^\"]*)\" using auth token \"([^\"]*)\"$",
//                (String createAccRes, String authToken) -> {
//                    String schema = "getAccountId";
//                    requestState.reset("istio");
//                    requestState.setResponseStateType(APIResponseStateType.valueOf("JSON"));
//                    Pair<ObjectNode, ObjectMapper> readerObjectMapperPair = YamlReader.readJsonSchemaFile(schema);
//                    Map<String, String> headers = readerObjectMapperPair.getValue().readValue(String.valueOf(readerObjectMapperPair.getKey().get("headers")),
//                            new TypeReference<Map<String, String>>() {
//                            });
//                    if (config.getBoolean("flags.staticAuth"))
//                        headers.put("Authorization", "Bearer VALID_STATIC_TOKEN");
//                    else
//                        headers.put("Authorization", paramTransformer.transform(authToken));
//                    headers.forEach(requestState::setHeader);
//                    requestState.setParameter("sortCode", paramTransformer.transform("{{response::" + createAccRes + "->SortCode}}"));
//                    requestState.setParameter("accountNumber", paramTransformer.transform("{{response::" + createAccRes + "->AccountNumber}}"));
//                    requestState.setHttpMethod("GET");
//                    String uri = new ObjectMapper().readValue(requestState
//                            .setSchemaWithTestData("baseuri", schema, "AccountIstio-baseuri"), ObjectNode.class)
//                            .get("basepath").textValue();
//                    requestState.setURI(uri);
//                    requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//                });
//
//        Given("^I onboard a new customer with data \"([^\"]*)\"$",
//                (String testData) -> {
//                    helper.initiateRequestAndSetParser();
//                    String schema = "customerOnboard";
//                    helper.setHeaderFromJson(schema);
//                    helper.removeHeader("X-Forwarded-Host");
//                    helper.setRequestBodyWithJsonYmlData(schema, testData);
//                    helper.setHttpMethodAndUriFromJson("/baseuri/basepath", "POST", schema);
//                    requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(201);
//                });
//
//        Given("^I update username \"([^\"]*)\" for the customer \"([^\"]*)\"$",
//                (String userName, String customerId) -> {
//                    if (!(config.getBoolean("flags.staticAuth"))) {
//                        helper.initiateRequestAndSetParser();
//                        String schema = "createUser";
//                        String actualCustId = paramTransformer.transform(customerId);
//                        helper.setHeaderFromYml(schema, "ValidUser-headers");
//                        String reqBody = helper.setJsonWithYmlData("request", schema, "ValidUser-request").replace("{customerid}", actualCustId);
//                        helper.setRequestBody(reqBody);
//                        helper.setPathParam("username", userName + RandomUtils.nextInt());
//                        helper.setHttpMethodAndUriFromJson("/baseuri/basepath", "PUT", schema);
//                        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(201);
//                    }});

//        Given("^I do first time login for the customer \"([^\"]*)\"$",
//                (String customerId) -> {
//                    if (!(config.getBoolean("flags.staticAuth"))) {
//                        helper.iDoFirstTimeLogin(customerId);
//                        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//                }});
//
//        Given("^I validate first time login with auth id \"([^\"]*)\", customer \"([^\"]*)\" and username \"([^\"]*)\"$",
//                (String authId, String customerId, String userName) -> {
//                    if (!(config.getBoolean("flags.staticAuth"))) {
//                        helper.iDoFirstTimeLoginWithUserName(authId, customerId, userName, "ValidUserTempPassword-request");
//                }});
//
//        Given("^I reset Password with auth id \"([^\"]*)\" for the customer \"([^\"]*)\"$",
//                (String authId, String customerId) -> {
//                    if (!(config.getBoolean("flags.staticAuth"))) {
//                        helper.iDoFirstTimeLoginWithoutUserName(authId, customerId, "ValidUserResetPassword-request");
//                }});
//
//        Given("^I create an MI with auth id \"([^\"]*)\" for the customer \"([^\"]*)\"$",
//                (String authId, String customerId) -> {
//                    if (!(config.getBoolean("flags.staticAuth"))) {
//                    helper.iDoFirstTimeLoginWithoutUserName(authId, customerId,"ValidUserMI-request");
//                }});
//
//        Given("^I generate auth token for the user \"([^\"]*)\"$",
//                (String testData) -> {
//                    if (!config.getBoolean("flags.staticAuth")) {
//                        // Get the Username for which the auth token needs to be generated
//                        String userName = helper.readYmlObjectToMap("authToken", testData).get("username");
//                        String password = helper.readYmlObjectToMap("authToken", testData).get("password");
//                        String customerId = helper.readYmlObjectToMap("authToken", testData).get("customerId");
//
//                        helper.iGenerateAuthTokenForGivenUsernamePassword(userName,customerId, password);
//                    }
//                });
        Given("^I fetch \"([^\"]*)\" from testdata for \"([^\"]*)\" to a file \"([^\"]*)\"$", (String tag, String testDataKey, String fileName) -> {
            JsonPath Json = JsonPath.from(new File(System.getProperty("user.dir") + File.separator + "testdata.json"));
            String testDataValue = Json.getString(testDataKey+ "." + tag);
            BufferedWriter out = new BufferedWriter(
                    new FileWriter(fileName, true));
            out.write(getCurrentDateTime("yyyy/MM/dd HH:mm:ss") + ": " + testDataValue + "\n");
            out.close();
            System.setProperty("customerId", testDataValue);
            });

//        Given("^I generate auth token for customer \"([^\"]*)\" with username \"([^\"]*)\" and password \"([^\"]*)\"$",
//                (String customerId, String userName, String password) -> {
//                    if (!config.getBoolean("flags.staticAuth")) {
//                        helper.iGenerateAuthTokenForGivenUsernamePassword(userName, customerId, password);
//                    }
//        });
//        Given("^I make an OnUs payment of \"([^\"]*)\" from \"([^\"]*)\" to \"([^\"]*)\" with AuthToken \"([^\"]*)\"$",
//                (String amt, String fromAcc, String toAcc, String authToken) -> {
//                    helper.iMakeAnOnUsPayment(amt, fromAcc, toAcc, authToken);
//                });
//
//        Given("^I add the account \"([^\"]*)\" as beneficiary for \"([^\"]*)\" with AuthToken \"([^\"]*)\"$",
//                (String toAcc, String fromAcc, String authToken) -> {
//                    helper.iAddBeneficiary(toAcc, fromAcc, authToken);
//                });
//
//        Given("^I add the external account \"([^\"]*)\" as beneficiary for \"([^\"]*)\" with AuthToken \"([^\"]*)\"$",
//                (String toAccTestData, String fromAcc, String authToken) -> {
//                    helper.iAddExternalAccAsBeneficiary(toAccTestData, fromAcc, authToken);
//                });
//
//        Given("^I make an FPS payment of \"([^\"]*)\" from \"([^\"]*)\" to \"([^\"]*)\" with AuthToken \"([^\"]*)\"$",
//                (String amt, String fromAcc, String toAccTestData, String authToken) -> {
//                    helper.iMakeAnFPSPayment(amt, fromAcc, toAccTestData, authToken);
//                });
        Given("^I set the system property \"([^\"]*)\" as \"([^\"]*)\" in the request body$",
                (String sysProp, String param) -> {
                    helper.replaceReqBodyFromSystemPropCustId(System.getProperty(sysProp), param);
        });
//        Given("^I create a mortgage repayment account for user \"([^\"]*)\"$",
//                (String user) -> {
//                    helper.createRepaymentAccount(user);
//                });
//        Given("^I create a mortgage sub account for user \"([^\"]*)\"$",
//                (String user) -> {
//                    helper.createSubAccount(user);
//                });
//        Given("^I create a mortgage sub account for user \"([^\"]*)\" with mortgage number \"([^\"]*)\"$",
//                (String user, String mortgageNumber) -> {
//                    helper.createSubAccountWithNumber(user,mortgageNumber);
//                });
//        Given("^I close a mortgage sub account for user \"([^\"]*)\" with accountid \"([^\"]*)\"$",
//                (String user, String id) -> {
//                    helper.closeAccount(user,id);
//                });
//        Given("^I create a mortgage repayment account for user \"([^\"]*)\" with vaultRequest \"([^\"]*)\"$",
//                (String user, String vaultRequest) -> {
//                    helper.createRepaymentAccountVault(user,vaultRequest);
//                });
//        Given("^I create a mortgage sub account for user \"([^\"]*)\" with mortgage number \"([^\"]*)\" with vaultRequest \"([^\"]*)\"$",
//                (String user, String mortgageNumber, String vaultRequest) -> {
//                    helper.createSubAccountWithNumberVault(user,mortgageNumber, vaultRequest);
//                });
    }

    @After
    public void afterScenario(Scenario scenario) {
        MDC.remove("curlLogDir");
        if (scenario.isFailed()) {
            String scenarioID = scenario.getId();
            String[] parts = scenarioID.split("[.;:/]");
            File log = new File(logFolder + "/" + parts[0] + "/" + parts[1] + "/" + parts[2] + "/" + parts[2] + parts[4] + ".log");
            byte[] byteData = new byte[0];
            try {
                byteData = Files.readAllBytes(log.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            scenario.embed(byteData, "text/plain");
        }
    }
}
