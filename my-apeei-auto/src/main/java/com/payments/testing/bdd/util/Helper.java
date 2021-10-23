package com.payments.testing.bdd.util;

import com.payments.testing.bdd.api.http.APIRequestState;
import com.payments.testing.bdd.api.http.APIResponseStateType;
import com.payments.testing.bdd.http.ResponseState;
import com.payments.testing.bdd.parameters.DefaultParamTransformer;
import com.typesafe.config.Config;
import io.cucumber.datatable.DataTable;
import io.cucumber.datatable.dependency.com.fasterxml.jackson.core.type.TypeReference;
import io.cucumber.datatable.dependency.com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.dependency.com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import javafx.util.Pair;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.restassured.path.xml.XmlPath;

import org.json.XML;

import static java.util.UUID.randomUUID;

public class Helper {
    private static Config config = BDDConfig.getConfig();
    public APIRequestState requestState;
    public DefaultParamTransformer paramTransformer;

    public Helper(APIRequestState requestState, DefaultParamTransformer paramTransformer) {
        this.requestState = requestState;
        this.paramTransformer = paramTransformer;
    }

    public void initiateRequestAndSetParser() {
        requestState.reset();
        setParser("JSON");
    }

    public void initiateRequestAndSetDefaultParser(String host) {
        requestState.reset(host);
        setParser("JSON");
    }

    public void initiateRequestAndSetParser(String type) {
        requestState.reset();
        setParser(type);
    }

    public void initiateRequestAndSetParser(String host, String type) {
        requestState.reset(host);
        setParser(type);
    }

    public void setURIAndMethod(String method, String uri) {
        requestState.setURI(paramTransformer.transform(uri));
        requestState.setHttpMethod(method);
    }

    public void setUriMethodAndHost(String method, String uri, String host) {
        setURIAndMethod(method, uri);
        requestState.setHost(host);
    }

    public void setParser(String type) {
        requestState.setResponseStateType(APIResponseStateType.valueOf(type));
    }

    public void setRequestBodyAndContentType(String type, String body) {
        requestState.setHeader("Content-Type", ContentType.valueOf(type).withCharset("utf-8"));
        requestState.setBody(paramTransformer.transform(body));
    }

    public void setRequestBody(String body) {
        requestState.setBody(paramTransformer.transform(body));
    }

    public void setRequestBodyAndContentType(String type, DataTable dataTable) {
        requestState.setHeader("Content-Type", ContentType.valueOf(type).withCharset("utf-8"));
        requestState.setBody(convertMapToString(convertDataTableToMap(dataTable)));
    }

    public void setRequestBodyWithJsonYmlData(String schema, String testData) throws IOException {
        requestState.setBody(setJsonWithYmlData("request", schema, testData));
    }

    public Map<String, String> convertDataTableToMap(DataTable table) {
        Map<String, String> props = table.asMap(String.class, String.class);
        Map<String, String> transformed = props.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> paramTransformer.transform(e.getValue())
                ));
        return transformed;
    }

    public String setJsonWithYmlData(String reqEntity, String schema, String testData) throws IOException {
        return requestState.setSchemaWithTestData(reqEntity, schema, testData);
    }

    public String setJsonWithSoapAuthTokenYmlData(String reqEntity, String schema, String testData) throws IOException {
        return requestState.setSchemaWithSOAPTestData(reqEntity, schema, testData);
    }

    public Map<String, String> setJsonWithYmlDataAsMap(String reqEntity, String schema, String testData) throws IOException {
        return requestState.setSchemaWithTestDataAsMap(reqEntity, schema, testData);
    }

    public Map<String, String> readYmlObjectToMap(String ymlFile, String testData) {
        YamlReader yamlReader = new YamlReader(ClassLoader.getSystemResourceAsStream(config
                .getConfig("files").getString("testdata").replace("{testdata_name}", ymlFile)));
        Map<String, Object> map = yamlReader.getYamlObj(testData);
        Map<String, String> transformed = map.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue().toString())
                );
        System.out.println(transformed);
        return transformed;
    }

    public String readYmlObjectToString(String ymlFile, String testData) {
        return convertMapToString(readYmlObjectToMap(ymlFile, testData));
    }

    public String convertMapToString(Map<String, String> tagValues) {
        return new PropertiesToJsonConverter().convertToJson(tagValues);
    }

    public String readJsonNodeValue(String schema, String jsonNode) throws IOException {
        Pair<ObjectNode, ObjectMapper> readerObjectMapperPair = YamlReader.readJsonSchemaFile(schema);
        return YamlReader.getJsonNodeValue(readerObjectMapperPair.getKey(), jsonNode);
    }

    public Map<String, String> readMapFromJson(String reqEntity, String schema) throws IOException {
        Pair<ObjectNode, ObjectMapper> readerObjectMapperPair = YamlReader.readJsonSchemaFile(schema);
        Map<String, String> map = readerObjectMapperPair.getValue().readValue(String.valueOf(readerObjectMapperPair.getKey().get(reqEntity)),
                new TypeReference<Map<String, String>>() {
                });
        return map;
    }

    public void setHttpMethodAndUriFromJson(String jsonNode, String method, String schema) throws IOException {
        requestState.setHttpMethod(method);
        requestState.setURI(readJsonNodeValue(schema, jsonNode));
    }

    public void setHttpMethodAndUriFromYmlFile(String requestEntity, String ymlNode, String method, String schema, String testData) throws IOException {
        requestState.setHttpMethod(method);
        String uri = new ObjectMapper().readValue(requestState
                .setSchemaWithTestData(requestEntity, schema, testData), ObjectNode.class)
                .get(ymlNode).textValue();
        requestState.setURI(uri);
    }

    public static String getCurrentDateTime(String pattern) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }

    public void setHeader(String name, String val) {
        name = paramTransformer.transform(name);
        val = paramTransformer.transform(val);
        requestState.setHeader(name, val);
    }

    public void setHeaders(DataTable dataTable) {
        dataTable
                .asMap(String.class, String.class)
                .forEach(
                        (name, val) -> {
                            setHeader((String) name, (String) val);
                        });
    }

    public void setHeaders(Map<String, String> headers) {
        requestState.setHeaders(headers);
    }

    public void setHeaders(String schema, String testData) throws IOException {
        setHeaders(setJsonWithYmlDataAsMap("headers", schema, testData));
    }

    public void setHeaderFromJson(String schema) throws IOException {
        setHeaders(readMapFromJson("headers", schema));
    }

    public void removeHeader(String header) throws IOException {
        requestState.getHeaders().remove(header);
    }

    public void setHeaderFromYml(String ymlFile, String data) {
        setHeaders(readYmlObjectToMap(ymlFile, data));
    }

    public void setContentType(String contentType) {
        setHeader("Content-Type", contentType);
    }

    public void setQueryParam(String name, String val) {
        name = paramTransformer.transform(name);
        val = paramTransformer.transform(val);
        requestState.setParameter(name, val);
    }

    public void setQueryParams(Map<String, String> queryParams) {
        queryParams.forEach(
                this::setQueryParam
        );
    }

    public void setQueryParams(DataTable dataTable) {
        dataTable
                .asMap(String.class, String.class)
                .forEach(
                        (name, val) -> {
                            setQueryParam((String) name, (String) val);
                        });
    }

    public void setQueryParamsFromFile(String ymlFile, String data) {
        setQueryParams(readYmlObjectToMap(ymlFile, data));
    }

    public void setQueryParamFromFile(String queryParam, String ymlFile, String testData) {
        String query = readYmlObjectToMap(ymlFile, testData).get(queryParam);
        setQueryParam(queryParam, query);
    }

    public void setPathParam(String name, String val) {
        String pathParamVal = paramTransformer.transform(val);
        requestState.setPathParameter(name, pathParamVal);
    }

    public void setPathParamFromFile(String pathParam, String ymlFile, String testData) {
        String value = readYmlObjectToMap(ymlFile, testData).get(pathParam);
        setPathParam(pathParam, value);
    }

    public void setFormParamsFromYml(String ymlFile, String testData) {
        Map<String, String> formParams = readYmlObjectToMap(ymlFile, testData);
        setFormParams(formParams);
    }

    public void setFormParams(Map<String, String> map) {
        map.forEach((name, val) -> {
            requestState.setFormParameter(name, val);
        });
    }

    public void savePdfToFolder(String fileName) throws IOException {
        byte[] pdf = requestState.getResponseState().getResponse().asByteArray();
        FileOutputStream outputImageFile = new FileOutputStream(
                System.getProperty("user.dir") + "/PDF/" + fileName + ".pdf");
        outputImageFile.write(pdf);
        outputImageFile.close();
    }
    public String getTextFromPDF(PDDocument doc) throws IOException {
        PDFTextStripper pdfStripper = new PDFTextStripper();
        return pdfStripper.getText(doc);
    }

    public String generateAuthTokan() throws IOException {
        String schema = "authToken";

        initiateRequestAndSetParser("XML");
        requestState.reset("authTokenHost");
        setContentType("application/xml");
        setHttpMethodAndUriFromJson("/baseuri/basepath", "POST", schema);
        String reqBody = setJsonWithSoapAuthTokenYmlData("request", schema, "GenerateAuthToken-request");
        setRequestBody(reqBody);
        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
        return "tokan";
    }

    public String validateAuthTokan() throws IOException {
        String authID = getSOAPAuthID("soapenv:Envelope/soapenv:Body/ws:AdministerBenefitArrangementResponse/informationContent/ri:content");
        String schema = "validateAuthToken";
        initiateRequestAndSetParser("JSON");
        setContentType("application/json");
        setHttpMethodAndUriFromJson("/baseuri/basepath", "POST", schema);
        String reqBody = setJsonWithYmlData("request", schema, "validateAuthToken-request")
                .replace("{tokenId}",authID);
        setRequestBody(reqBody);
        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);

        return "tokan";
    }

    public String getSOAPAuthID(String xmlPath){
        return getXMLPathValue(xmlPath);
    }

    public String getXMLPathValue(String path) {
        String resBody = requestState.getResponseState().getResponse().body().prettyPrint();
        XMLReader xmlReader = new XMLReader(resBody);
        return xmlReader.getText(path);
    }

    public void iGenerateMyAuthToken(Map<String, String> cookies, String code) throws IOException {
        String schema = "authToken";
        initiateRequestAndSetParser();
        setContentType("application/x-www-form-urlencoded");
        //requestState.setCookies(cookies);
        //setHeader("iPlanetDirectoryPro", cookies.get("iPlanetDirectoryPro"));
        setHeader("iPlanetDirectoryPro", code);
        Map<String, String> formParams = readYmlObjectToMap(schema, "GenerateAuthToken-request");
        formParams.replace("code", code);
        setFormParams(formParams);
        setHttpMethodAndUriFromJson("/baseuri/basepath", "POST", schema);
        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
    }





//    public void iDoFirstTimeLogin(String customerId) throws IOException {
//        String schema = "firstTimeLogin";
//        initiateRequestAndSetParser();
//        setHeaderFromJson(schema);
//        setHeader("x-lbg-txn-correlation-id", customerId);
//        setHeader("x-lbg-customer-id", customerId);
//        setHttpMethodAndUriFromJson("/baseuri/basepath", "POST", schema);
//    }
//
//    public void iDoFirstTimeLoginWithUserName(String authId,String customerId, String userName, String testData) throws IOException {
//        String schema = "firstTimeLogin";
//        iDoFirstTimeLogin(customerId);
//        String reqBody = setJsonWithYmlData("request", schema, testData)
//                .replace("{authId}", paramTransformer.transform(authId))
//                .replace("{username}", paramTransformer.transform(userName));
//        setRequestBody(reqBody);
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//    }
//
//    public void iValidateUserNameAndPassword(String authId, String customerId, String userName, String password, String testData) throws IOException {
//        String schema = "firstTimeLogin";
//        iDoFirstTimeLogin(customerId);
//        String reqBody = setJsonWithYmlData("request", schema, testData)
//                .replace("{authId}", paramTransformer.transform(authId))
//                .replace("{username}", paramTransformer.transform(userName))
//                .replace("{password}", paramTransformer.transform(password));
//        setRequestBody(reqBody);
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//    }
//
//    public void iDoFirstTimeLoginWithoutUserName(String authId,String customerId,  String testData) throws IOException {
//        String schema = "firstTimeLogin";
//        iDoFirstTimeLogin(customerId);
//        String reqBody = setJsonWithYmlData("request", schema, testData)
//                .replace("{authId}", paramTransformer.transform(authId));
//        setRequestBody(reqBody);
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//    }
//
//    public void iInvokeAuthCallByChannel() throws IOException {
//        String schema = "authCallByChannel";
//        initiateRequestAndSetParser();
//        setQueryParamsFromFile(schema, "ValidRequest-QueryParams");
//        setHttpMethodAndUriFromJson("/baseuri/basepath", "GET", schema);
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(302);
//    }
//
//    public void iDoMyActualAuthCall(String state, String csrf, Map<String, String> cookies) throws IOException {
//        String schema = "authCallByChannel";
//        initiateRequestAndSetParser();
//        setContentType("application/x-www-form-urlencoded");
//        requestState.setCookies(cookies);
//        setQueryParam("state", state);
//        Map<String, String> map = new HashMap<>();
//        map.put("csrf", csrf);
//        setFormParams(map);
//        setHttpMethodAndUriFromJson("/baseuri/basepath", "POST", schema);
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(302);
//    }

//    public void iGenerateMyAuthToken(Map<String, String> cookies, String code) throws IOException {
//        String schema = "authToken";
//        initiateRequestAndSetParser();
//        setContentType("application/x-www-form-urlencoded");
//        //requestState.setCookies(cookies);
//        //setHeader("iPlanetDirectoryPro", cookies.get("iPlanetDirectoryPro"));
//        setHeader("iPlanetDirectoryPro", code);
//        Map<String, String> formParams = readYmlObjectToMap(schema, "GenerateAuthToken-request");
//        formParams.replace("code", code);
//        setFormParams(formParams);
//        setHttpMethodAndUriFromJson("/baseuri/basepath", "POST", schema);
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//    }

//    public void iDoMyLoginWithMI(String authId, String customerId, ArrayList<Object> pos, ArrayList<String> values) throws IOException {
//        iDoFirstTimeLogin(customerId);
//        String reqBody = setJsonWithYmlData("request", "firstTimeLogin", "MIValidation-request")
//                .replace("{authId}", authId)
//                .replaceAll("\\{pos1}", pos.get(0).toString())
//                .replaceAll("\\{pos2}", pos.get(1).toString())
//                .replaceAll("\\{pos3}", pos.get(2).toString())
//                .replaceAll("\\{val1}", values.get(0))
//                .replaceAll("\\{val2}", values.get(1))
//                .replaceAll("\\{val3}", values.get(2));
//        setRequestBody(reqBody);
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//    }

    public String getJsonPathValue(String path) {
        return requestState.getResponseState().getResponse().jsonPath().get(path);
    }

//    public String getAuthId() {
//        return getJsonPathValue("authId");
//    }

//    public ArrayList<String> getValuesForPosForMI(ArrayList<Object> pos) {
//        ArrayList<String> values = new ArrayList<>();
//        pos.forEach(
//                (val) -> {
//                    if (val.equals(1)) {
//                        values.add("A");
//                    } else {
//                        values.add(val.toString());
//                    }
//                });
//        return values;
//    }

//    public void createAccount(String schema, String authToken, String testData) throws IOException {
//        setHeaderFromJson(schema);
//        if (config.getBoolean("flags.staticAuth"))
//            setHeader("Authorization", "Bearer VALID_STATIC_TOKEN");
//        else
//            setHeader("Authorization", paramTransformer.transform(authToken));
//        setRequestBodyWithJsonYmlData(schema, testData + "-request");
//        setHttpMethodAndUriFromYmlFile("baseuri", "basepath", "POST", schema, testData + "-baseuri");
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//    }
//
//    public void iUpdateAccount(String accountId, String testData, String authToken) throws IOException {
//        String accountIdNew = paramTransformer.transform(accountId);
//        String schema = "updateAccount";
//        initiateRequestAndSetParser();
//        Map<String, String> headers = setJsonWithYmlDataAsMap("headers", schema, testData + "-headers");
//        if (config.getBoolean("flags.staticAuth"))
//            headers.put("Authorization", "Bearer VALID_STATIC_TOKEN");
//        else
//            headers.put("Authorization", paramTransformer.transform(authToken));
//        headers.forEach(requestState::setHeader);
//        setPathParam("accountid", accountIdNew);
//        setRequestBodyWithJsonYmlData(schema, testData + "-request");
//        setHttpMethodAndUriFromYmlFile("baseuri", "basepath", "PUT", schema, "Account1-baseuri");
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//    }
//
//    public void iGenerateAuthTokenForGivenUsernamePassword(String userName,String customerId, String password) throws IOException {
//        // Call 1 - to invoke Auth Call by Channel and get the state
//        iInvokeAuthCallByChannel();
//        String state = requestState.getResponseState().getResponse().header("Location").split("goto=")[1].split("&")[0];
//
//        // Call 2 - Invoke Tree to generate auth token required in the next call
//        iDoFirstTimeLogin(customerId);
//        String authId = getAuthId();
//
//        // Call 3 - Validate the Username and Password
//        iValidateUserNameAndPassword(authId, customerId, userName, password, "ValidateLoginUsernameAndPassword-request");
//        authId = getAuthId();
//
//        // String manipulation required to get the Postions and the Values for MI
//        String value = requestState.getResponseState().getResponse().jsonPath().get("callbacks[0].output[0].value");
//        String miChallengeInformation = JsonPath.from(value).getString("nextStateValue");
//        ArrayList<Object> pos = (ArrayList<Object>) JsonPath.from(miChallengeInformation).getList("challengeInformation.pos");
//        ArrayList<String> values = getValuesForPosForMI(pos);
//
//        // Call 4 - Validate the MI
//        iDoMyLoginWithMI(authId, customerId, pos, values);
//        Map<String, String> cookies = requestState.getResponseState().getResponse().getCookies();
//        String csfr = getJsonPathValue("tokenId");
//
//        // Call 5 - Actual Auth Call
//        iDoMyActualAuthCall(state, csfr, cookies);
//        Map<String, String> cookies1 = requestState.getResponseState().getResponse().getCookies();
//        String code = requestState.getResponseState().getResponse().header("Location").split("code=")[1].split("&iss=")[0];
//
//        // Call 6 - Access Token Generation
//        iGenerateMyAuthToken(cookies1, code);
//    }
//
//    public void iMakeAnOnUsPayment(String amt, String fromAcc, String toAcc, String authToken) throws IOException {
//        if (config.getBoolean("flags.staticAuth")) {
//            iMakePaymentViaVault(amt, fromAcc, toAcc);
//        } else {
//            String schema = "payment";
//            iMakePaymentInitiateReq(schema, authToken);
//            setRequestBodyWithJsonYmlData(schema, "Payment-request");
//            requestState.setBody(requestState.getBody().replaceAll("\\{DASALiteAccountNumber1}", paramTransformer.transform(fromAcc)));
//            requestState.setBody(requestState.getBody().replaceAll("\\{DASALiteAccountNumber2}", paramTransformer.transform(toAcc)));
//            requestState.setBody(requestState.getBody().replaceAll("\\{Amount}", paramTransformer.transform(amt)));
//            setHttpMethodAndUriFromJson("/baseuri/basepath", "POST", schema);
//            requestState.getResponseState().getResponse().then().statusCode(201);
//        }
//    }
//
//    public void iMakeAnFPSPayment(String amt, String fromAcc, String toAccTestData, String authToken) throws IOException {
//        if (config.getBoolean("flags.staticAuth")) {
//            iUpdateVaultPayment(iInitiateFPSVaultPayment(amt, fromAcc, toAccTestData));
//        } else {
//            String schema = "payment";
//            iMakePaymentInitiateReq(schema, authToken);
//            setRequestBodyWithJsonYmlData(schema, toAccTestData);
//            requestState.setBody(requestState.getBody().replaceAll("\\{DASALiteAccountNumber1}", paramTransformer.transform(fromAcc)));
//            requestState.setBody(requestState.getBody().replaceAll("\\{Amount}", paramTransformer.transform(amt)));
//            setHttpMethodAndUriFromJson("/baseuri/basepath", "POST", schema);
//            requestState.getResponseState().getResponse().then().statusCode(201);
//        }
//    }
//
//    public void iMakePaymentInitiateReq(String schema, String authToken) throws IOException {
//        initiateRequestAndSetParser();
//        Map<String, String> headers = readMapFromJson("headers", schema);
//        headers.put("Authorization", paramTransformer.transform(authToken));
//        headers.put("x-lbg-txn-correlation-id", randomUUID().toString());
//        headers.put("x-idempotency-key", randomUUID().toString());
//        setHeaders(headers);
//    }
//
//    public void iAddBeneficiary(String toAcc, String fromAcc, String authToken) throws IOException {
//        if (config.getBoolean("flags.staticAuth")) {
//            System.out.println("Vault APIs doesn't require Beneficiary");
//        } else {
//            String schema = "beneficiary";
//            iAddBeneficiaryInitiateReq(fromAcc, authToken, schema);
//            setRequestBodyWithJsonYmlData(schema, "AddBeneficiary-request");
//            requestState.setBody(requestState.getBody().replaceAll("\\{DASALiteAccountNumber2}", paramTransformer.transform(toAcc)));
//            setHttpMethodAndUriFromJson("/baseuri/basepath", "POST", schema);
//            requestState.getResponseState().getResponse().then().statusCode(200);
//        }
//    }
//
//    public void iAddExternalAccAsBeneficiary(String toAccTestData, String fromAcc, String authToken) throws IOException {
//        if (config.getBoolean("flags.staticAuth")) {
//            System.out.println("Vault APIs doesn't require Beneficiary");
//        } else {
//            String schema = "beneficiary";
//            iAddBeneficiaryInitiateReq(fromAcc, authToken, schema);
//            setRequestBodyWithJsonYmlData(schema, toAccTestData);
//            setHttpMethodAndUriFromJson("/baseuri/basepath", "POST", schema);
//            requestState.getResponseState().getResponse().then().statusCode(200);
//        }
//    }
//
//    public void iAddBeneficiaryInitiateReq(String fromAcc, String authToken, String schema) throws IOException {
//        initiateRequestAndSetParser();
//        Map<String, String> headers = readMapFromJson("headers", schema);
//        headers.put("Authorization", paramTransformer.transform(authToken));
//        headers.put("x-lbg-txn-correlation-id", randomUUID().toString());
//        setHeaders(headers);
//        setPathParam("accountid", paramTransformer.transform(fromAcc));
//    }
//
//    public void iMakePaymentViaVault(String amt, String fromAcc, String toAcc) throws IOException {
//        iUpdateVaultPayment(iInitiateVaultPayment(amt, fromAcc, toAcc));
//    }
//
//    public String iInitiateVaultPayment(String amt, String fromAcc, String toAcc) throws IOException {
//        String schema = "createVaultPayment";
//        initiateRequestAndSetDefaultParser("vaultpayments");
//        requestState.setProxy("",0);
//        setHeaders(schema, "Vault-headers");
//        setRequestBodyWithJsonYmlData(schema, "Payment-request");
//        requestState.setBody(requestState.getBody().replaceAll("\\{DASALiteAccountNumber1}", paramTransformer.transform(fromAcc)));
//        requestState.setBody(requestState.getBody().replaceAll("\\{DASALiteAccountNumber2}", paramTransformer.transform(toAcc)));
//        requestState.setBody(requestState.getBody().replaceAll("\\{Amount}", paramTransformer.transform(amt)));
//        setHttpMethodAndUriFromJson("/baseuri/basepath", "POST", schema);
//        requestState.getResponseState().getResponse().then().statusCode(200);
//        return requestState.getResponseState().getResponse().getBody().jsonPath().get("id");
//    }
//
//    public String iInitiateFPSVaultPayment(String amt, String fromAcc, String toAccTestData) throws IOException {
//        String schema = "createVaultPayment";
//        initiateRequestAndSetDefaultParser("vaultpayments");
//        requestState.setProxy("",0);
//        setHeaders(schema, "Vault-headers");
//        setRequestBodyWithJsonYmlData(schema, toAccTestData);
//        requestState.setBody(requestState.getBody().replaceAll("\\{DASALiteAccountNumber1}", paramTransformer.transform(fromAcc)));
//        requestState.setBody(requestState.getBody().replaceAll("\\{Amount}", paramTransformer.transform(amt)));
//        setHttpMethodAndUriFromJson("/baseuri/basepath", "POST", schema);
//        requestState.getResponseState().getResponse().then().statusCode(200);
//        return requestState.getResponseState().getResponse().getBody().jsonPath().get("id");
//    }
//
//    public void iUpdateVaultPayment(String id) throws IOException {
//        String schema = "createVaultPayment";
//        initiateRequestAndSetDefaultParser("vaultpayments");
//        requestState.setProxy("",0);
//        setHeaders(schema, "Vault-headers");
//        setRequestBody(readYmlObjectToString(schema, "InitiatePayment-request")
//                .replaceAll("\\{RandomUUID}", randomUUID().toString()));
//        setPathParam("id", id);
//        setHttpMethodAndUriFromYmlFile("baseuri", "basepath", "POST", schema, "InitiatePayment-baseuri");
//        requestState.getResponseState().getResponse().then().statusCode(200);
//    }
//
//
    public void replaceValueInReqBody(String param, String ymlFile, String testData) {
        String paramValue = readYmlObjectToMap(ymlFile, testData).get(param);
        replaceReqBodyFromSystemPropCustId(paramValue, param);
    }
////
    public void replaceReqBodyFromSystemPropCustId(String paramValue, String param) {
        String body = requestState.getBody().replaceAll("\\{\"" + param + "\":null}", "\"" + paramValue + "\"")
                .replaceAll("\n", "")
                .replaceAll("\\{    \"" + param + "\": null  }", "\"" + paramValue + "\"")
                .replaceAll("\\{" + param + "}", paramValue);
        setRequestBody(body);
    }
//
//
//    public void createRepaymentAccount(String user) throws IOException
//    {
//        String schema = "VaultCreate";
//        String mortgageNumber = randomUUID().toString();
//        initiateRequestAndSetDefaultParser("vaultcore");
//        requestState.setProxy("",0);
//        setHeaders(schema, "Vault-headers");
//        setRequestBody(setJsonWithYmlData("request", schema, "RepaymentAccount-request")
//                .replaceAll("\\{RandomUUID}", randomUUID().toString())
//                .replaceAll("\\{vaultCustomerId}",readYmlObjectToMap("authToken", user).get("vaultCustomerId"))
//                .replaceAll("\\{mortgageNumber}",mortgageNumber));
//        setHttpMethodAndUriFromYmlFile("baseuri", "basepath", "POST", schema, "Vault-baseuri");
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//    }
//    public void closeAccount(String user, String id) throws IOException, InterruptedException
//    {
//        String schema = "VaultClose";
//        initiateRequestAndSetDefaultParser("vaultcore");
//        requestState.setProxy("",0);
//        setHeaders(schema, "Vault-headers");
//        setRequestBody(setJsonWithYmlData("request", schema, "PendingClosure-Request")
//                .replaceAll("\\{RandomUUID}", randomUUID().toString())
//                .replaceAll("\\{vaultCustomerId}",readYmlObjectToMap("authToken", user).get("vaultCustomerId")));
//        setPathParam("accountid",id);
//        setHttpMethodAndUriFromYmlFile("baseuri", "basepath", "PUT", schema, "Vault-baseuri");
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//        TimeUnit tu = TimeUnit.valueOf("SECONDS");
//        tu.sleep(10);
//        initiateRequestAndSetDefaultParser("vaultcore");
//        requestState.setProxy("",0);
//        setHeaders(schema, "Vault-headers");
//        setRequestBody(setJsonWithYmlData("request", schema, "Closed-Request")
//                        .replaceAll("\\{RandomUUID}", randomUUID().toString())
//                        .replaceAll("\\{vaultCustomerId}",readYmlObjectToMap("authToken", user).get("vaultCustomerId")));
//        setPathParam("accountid",id);
//        setHttpMethodAndUriFromYmlFile("baseuri", "basepath", "PUT", schema, "Vault-baseuri");
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//    }
//    public void createSubAccount(String user) throws IOException
//    {
//        String schema = "VaultCreate";
//        String mortgageNumber = randomUUID().toString();
//        initiateRequestAndSetDefaultParser("vaultcore");
//        requestState.setProxy( "",0);
//        setHeaders(schema, "Vault-headers");
//        setRequestBody(setJsonWithYmlData("request", schema, "SubAccount-request")
//                .replaceAll("\\{RandomUUID}", randomUUID().toString())
//                .replaceAll("\\{vaultCustomerId}",readYmlObjectToMap("authToken", user).get("vaultCustomerId"))
//                .replaceAll("\\{mortgageNumber}",mortgageNumber));
//        setHttpMethodAndUriFromYmlFile("baseuri", "basepath", "POST", schema, "Vault-baseuri");
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//    }
//    public void createSubAccountWithNumber(String user,String mortgageNumber) throws IOException
//    {
//        String schema = "VaultCreate";
//        String mortgageNumberValue = paramTransformer.transform(mortgageNumber);
//        initiateRequestAndSetDefaultParser("vaultcore");
//        requestState.setProxy("",0);
//        setHeaders(schema, "Vault-headers");
//        setRequestBody(setJsonWithYmlData("request", schema, "SubAccount-request")
//                .replaceAll("\\{RandomUUID}", randomUUID().toString())
//                .replaceAll("\\{vaultCustomerId}",readYmlObjectToMap("authToken", user).get("vaultCustomerId"))
//                .replaceAll("\\{mortgageNumber}",mortgageNumberValue));
//        setHttpMethodAndUriFromYmlFile("baseuri", "basepath", "POST", schema, "Vault-baseuri");
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//    }
//    public void createRepaymentAccountVault(String user, String testData) throws IOException
//    {
//        String schema = "VaultCreate";
//        String mortgageNumber = randomUUID().toString();
//        initiateRequestAndSetDefaultParser("vaultcore");
//        requestState.setProxy("",0);
//        setHeaders(schema, "Vault-headers");
//        setRequestBody(setJsonWithYmlData("request", schema, testData)
//                .replaceAll("\\{RandomUUID}", randomUUID().toString())
//                .replaceAll("\\{vaultCustomerId}",readYmlObjectToMap("authToken", user).get("vaultCustomerId"))
//                .replaceAll("\\{mortgageNumber}",mortgageNumber));
//        setHttpMethodAndUriFromYmlFile("baseuri", "basepath", "POST", schema, "Vault-baseuri");
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//    }
//    public void createSubAccountWithNumberVault(String user,String mortgageNumber, String testData) throws IOException
//    {
//        String schema = "VaultCreate";
//        String mortgageNumberValue = paramTransformer.transform(mortgageNumber);
//        initiateRequestAndSetDefaultParser("vaultcore");
//        requestState.setProxy("",0);
//        setHeaders(schema, "Vault-headers");
//        setRequestBody(setJsonWithYmlData("request", schema, testData)
//                .replaceAll("\\{RandomUUID}", randomUUID().toString())
//                .replaceAll("\\{vaultCustomerId}",readYmlObjectToMap("authToken", user).get("vaultCustomerId"))
//                .replaceAll("\\{mortgageNumber}",mortgageNumberValue));
//        setHttpMethodAndUriFromYmlFile("baseuri", "basepath", "POST", schema, "Vault-baseuri");
//        requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().statusCode(200);
//    }
}