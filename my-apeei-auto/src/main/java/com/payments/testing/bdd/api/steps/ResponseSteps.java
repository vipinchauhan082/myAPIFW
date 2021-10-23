package com.payments.testing.bdd.api.steps;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.payments.testing.bdd.api.http.APIRequestState;
import com.payments.testing.bdd.comparison.Operator;
import com.payments.testing.bdd.parameters.DefaultParamTransformer;
import com.payments.testing.bdd.util.*;
import com.typesafe.config.Config;
import cucumber.api.java8.En;
import io.cucumber.datatable.DataTable;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.hamcrest.Matchers;
import org.junit.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static com.payments.testing.bdd.util.GetImageLocationsAndSize.imageLocAndSizeList;
import static com.payments.testing.bdd.util.GetImageLocationsAndSize.pageCount;
import static com.payments.testing.bdd.util.GetWordsFromPDF.lineNumberCheck;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

public class ResponseSteps implements En {
    private static Config config = BDDConfig.getConfig();
    private PDDocument doc;
    GetImageLocationsAndSize getImageLocationsAndSize = new GetImageLocationsAndSize();
    GetWordsFromPDF getWordsFromPDF = new GetWordsFromPDF();

    /**
     * container for cucumber lambda methods.
     *
     * @param requestState     injected object
     * @param paramTransformer injected object
     */
    public ResponseSteps(APIRequestState requestState, DefaultParamTransformer paramTransformer) throws IOException {
        Helper helper = new Helper(requestState, paramTransformer);

        /**
         * cache the result for later use.
         *
         * @param id the id for the response
         */
        Then(
                "record the response as {string}",
                (String id) -> {

                        paramTransformer.cache(id, requestState.getResponseState(paramTransformer.getCachedObject("log")));
                });

        /**
         * pulls the response from host and matches statusCode.
         *
         * @param statusCode the status of the request as returned by the endpoint
         */
        Then(
                "I should get a status code of {int}",
                (Integer statusCode) -> {
                    Integer status = requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().getStatusCode();
                    assertEquals(
                            String.format(
                                    "Expected status code of %d, but got %d from %s",
                                    statusCode, status, requestState.getURI()),
                            statusCode,
                            status);
                });

        /**
         * executes a query against the response, which should return either true or false
         *
         * @param query the query to be executed
         * @param result either true or false
         */
        Then(
                "^evaluating \"([^\"]*)\" should return (true|false)$",
                (String query, String result) -> {
                    query = paramTransformer.transform(query);
                    requestState.getResponseState(paramTransformer.getCachedObject("log")).matches(query, Boolean.valueOf(result));
                });

        /**
         * element should/not equal value.
         *
         * @param path the query path (json, xml, etc...)
         * @param val the value to match against the path val
         */
        Then(
//        The default expression was as follows; it has been modified to compare string parameters that may have a " mark
//        "^the response value of \"([^\"]*)\" (should|should not) equal \"([^\"]*)\"$",
                "^the response value of \"([^\"]*)\" (should|should not) equal \"(.*)\"$",
                (String path, String operation, String val) -> {
                    Object in;
                    switch (val) {
                        case "TRUE":
                        case "FALSE":
                            in = Boolean.valueOf(val);
                            break;
                        default:
                            in = paramTransformer.transform(val);
                            break;
                    }

                    switch (operation) {
                        case "should not":
                            requestState.getResponseState(paramTransformer.getCachedObject("log")).notMatches(paramTransformer.transform(path), in);
                            break;
                        default:
                            requestState.getResponseState(paramTransformer.getCachedObject("log")).matches(paramTransformer.transform(path), in);
                            break;
                    }
                });


        /**
         * element should/not equal value.
         *
         * @param path the query path (json, xml, etc...)
         * @param val the value to match against the path val
         */
        Then(
                "^the response value of \"([^\"]*)\" (should|should not) equal (integer|float|long) \"([^\"]*)\"$",
                (String path, String operation, String type, String val) -> {
                    Object in = null;
                    switch (type) {
                        case "integer":
                            in = Integer.valueOf(val);
                            break;
                        case "float":
                            in = Float.valueOf(val);
                            break;
                        case "long":
                            in = Long.valueOf(val);
                            break;
                        default:
                            in = null;
                            break;
                    }

                    switch (operation) {
                        case "should not":
                            requestState.getResponseState(paramTransformer.getCachedObject("log")).notMatches(paramTransformer.transform(path), in);
                            break;
                        default:
                            requestState.getResponseState(paramTransformer.getCachedObject("log")).matches(paramTransformer.transform(path), in);
                            break;
                    }
                });

        /**
         * element should greater/less than value.
         *
         * @param path the query path (json, xml, etc...)
         * @param integer val the value to match against the path val
         */
        Then(
                "^the response value of \"([^\"]*)\" (less|greater) than (\\d+)$",
                (String path, String operation, Integer val) -> {
                    switch (operation) {
                        case "greater":
                            requestState.getResponseState().greaterThan(paramTransformer.transform(path), val);
                            break;
                        default:
                            requestState.getResponseState().lessThan(paramTransformer.transform(path), val);
                            break;
                    }
                });

        /**
         * check path for number of occurances
         *
         * @param path the query path
         * @param comparator the comparator
         * @param cnt the number of occurrences
         */
        Then(
                "^path \"([^\"]*)\" must occur (only|more than|at least|less than|at most) (\\d+) times$",
                (String path, String comparator, Integer cnt) -> {
                    comparator = comparator.replace(' ', '_');
                    requestState.getResponseState(paramTransformer.getCachedObject("log")).hasXElements(path, Operator.valueOf(comparator), cnt);
                });

        /**
         * check path for number of occurrences with particular value (string/true|false)
         *
         * @param value the actual value
         * @param comparator the comparator
         * @param cnt the number of occurrences
         * @param path the query path
         */
        Then(
                "^the value \"([^\"]*)\" must occur (only|more than|at least|less than|at most) (\\d+) times for \"([^\"]*)\"$",
                (String val, String comparator, Integer cnt, String path) -> {
                    comparator = comparator.replace(' ', '_');
                    Object in;
                    switch (val) {
                        case "TRUE":
                        case "FALSE":
                            in = Boolean.valueOf(val);
                            break;
                        default:
                            in = paramTransformer.transform(val);
                            break;
                    }

                    requestState
                            .getResponseState(paramTransformer.getCachedObject("log"))
                            .hasXElementsWithVal(path, Operator.valueOf(comparator), cnt, in);
                });

        /**
         * check path for number of occurrences with particular value (integer, float, long)
         *
         * @param value the actual value
         * @param type the number type
         * @param comparator the comparator
         * @param cnt the number of occurrences
         * @param path the query path
         */
        Then(
                "^the (integer|float|long) \"([^\"]*)\" must occur (only|more than|at least|less than|at most) (\\d+) times for \"([^\"]*)\"$",
                (String type, String val, String comparator, Integer cnt, String path) -> {
                    comparator = comparator.replace(' ', '_');
                    Object in = null;
                    switch (type) {
                        case "integer":
                            in = Integer.valueOf(val);
                            break;
                        case "float":
                            in = Float.valueOf(val);
                            break;
                        case "long":
                            in = Long.valueOf(val);
                            break;
                        default:
                            break;
                    }

                    requestState
                            .getResponseState(paramTransformer.getCachedObject("log"))
                            .hasXElementsWithVal(path, Operator.valueOf(comparator), cnt, in);
                });

        /**
         * does the path have duplicates
         *
         * @param path the query path
         * @param type the comparator
         */
        Then(
                "^path \"([^\"]*)\" (does|does not) contain duplicates$",
                (String path, String type) -> {
                    requestState.getResponseState(paramTransformer.getCachedObject("log")).hasDuplicates(path, Objects.equals("does", type));
                });

        /**
         * does (or does not) contain element
         *
         * @param operation whether it should or shouldn't exist
         * @param table a table of elements to validate
         */
        Then(
                "^the response (should|should not) contain the following elements$",
                (String operation, DataTable table) -> {
                    table
                            .asList(String.class)
                            .forEach(
                                    path -> {
                                        String val = paramTransformer.transform((String) path);
                                        switch (operation) {
                                            case "should not":
                                                requestState.getResponseState(paramTransformer.getCachedObject("log")).hasXElements(val, Operator.only, 0);
                                                break;
                                            default:
                                                requestState.getResponseState(paramTransformer.getCachedObject("log")).hasXElements(val, Operator.at_least, 1);
                                                break;
                                        }
                                    });
                });

        Then(
                "^the response (should|should not) contain the following non-empty elements$",
                (String operation, DataTable table) -> {
                    table
                            .asList(String.class)
                            .forEach(
                                    path -> {
                                        String val = paramTransformer.transform((String) path);
                                        switch (operation) {
                                            case "should not":
                                                requestState.getResponseState(paramTransformer.getCachedObject("log")).hasXElements(val, Operator.only, 0);
                                                break;
                                            default:
                                                requestState.getResponseState(paramTransformer.getCachedObject("log")).hasXElements(val, Operator.at_least, 1);
                                                assertFalse(requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().jsonPath().getList(val).isEmpty());
                                                break;
                                        }
                                    });
                });
        /**
         * the path values match (or don't).
         *
         * @param path1 the first path
         * @param op should or shouldn't match
         * @param path2 the second path
         */
        Then(
                "^path value \"([^\"]*)\" should (equal|not equal) \"([^\"]*)\" value$",
                (String path1, String op, String path2) -> {
                    path1 = paramTransformer.hasToken(path1) ? paramTransformer.transform(path1) : path1;
                    path2 = paramTransformer.hasToken(path2) ? paramTransformer.transform(path2) : path2;
                    requestState.getResponseState(paramTransformer.getCachedObject("log")).elementsMatch(path1, path2, op.replace(' ', '_'));
                });

        /**
         * the element values match (or don't).
         *
         * @param op whether should or shouldn't match
         * @param datatable list of path pairs
         */
        Then(
                "^the following path values should (equal|not equal) each other$",
                (String op, DataTable dt) -> {
                    Map<String, String> paths = dt.asMap(String.class, String.class);
                    paths.forEach(
                            (path1, path2) -> {
                                path1 =
                                        paramTransformer.hasToken(path1) ? paramTransformer.transform(path1) : path1;
                                path2 =
                                        paramTransformer.hasToken(path2) ? paramTransformer.transform(path2) : path2;
                                requestState.getResponseState(paramTransformer.getCachedObject("log")).elementsMatch(path1, path2, op.replace(' ', '_'));
                            });
                });

        Then("^the response tag \"([^\"]*)\" should be empty$",
                (String tag) -> {
                    assertTrue(requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().jsonPath().getList(tag).isEmpty());
                });

        Then("^the response tag \"([^\"]*)\" should not be empty$",
                (String tag) -> {
                    assertFalse(requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().jsonPath().getList(tag).isEmpty());
                });

        Then("^the response value of key \"([^\"]*)\" should not be empty$",
                (String tag) -> {
                    assertFalse(requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().jsonPath().get(tag).toString().isEmpty());
                });

        Then(
//        The default expression was as follows; it has been modified to compare string parameters that may have a " mark
//        "^the response value of \"([^\"]*)\" (should|should not) equal \"([^\"]*)\"$",
                "^the response value of \"([^\"]*)\" should equal to the string \"(.*)\"$",
                (String path, String val) -> {
                    Object in;
                    switch (val) {
                        case "TRUE":
                        case "FALSE":
                            in = Boolean.valueOf(val);
                            break;
                        default:
                            in = paramTransformer.transform(val);
                            break;
                    }
                    String pathValue = requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().jsonPath().get(path).toString();
                    assertEquals(pathValue, in.toString());
                });

        Then(
                "^the response value of \"([^\"]*)\" should equal \"(.*)\" from \"(.*)\" in \"(.*)\" yml$",
                (String path, String value, String data, String file) -> {
                    Object in;
                    YamlReader yamlReader = new YamlReader(ClassLoader.getSystemResourceAsStream(config
                            .getConfig("files").getString("testdata").replace("{testdata_name}", file)));
                    Map<String, Object> selectData = yamlReader.getYamlObj(data);
                    String val = (String) selectData.get(value);
                    switch (val) {
                        case "TRUE":
                        case "FALSE":
                            in = Boolean.valueOf(val);
                            break;
                        default:
                            in = paramTransformer.transform(val);
                            break;
                    }
                    String pathValue = requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().jsonPath().get(path).toString();
                    assertEquals(pathValue, in.toString());
                });
        Then("^the response value of \"([^\"]*)\" should contain \"([^\"]*)\"$",
                (String path, String value) -> {
                    requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().then().body(path, containsString(value));
                });

        Then("^the response tag \"([^\"]*)\" should be sorted in \"([^\"]*)\" order$",
                (String path, String order) -> {
                    List<String> tagValues = requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().jsonPath().getList(path);
                    List<String> copy = new ArrayList<>(tagValues);
                    switch (order) {
                        case "ascending":
                            copy.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                            assertEquals(copy, tagValues);
                            break;
                        case "descending":
                            copy.sort(Comparator.nullsLast(Comparator.reverseOrder()));
                            break;
                        default:
                            Assert.fail("The response tags are not sorted");
                            break;
                    }

                });

        Then(
                "^path \"([^\"]*)\" must contain one of the following$",
                (String path, DataTable table) -> {
                    // Single element list.
                    List<String> values = (List<String>) requestState.getResponseState(paramTransformer.getCachedObject("log")).getValues(path);
                    List<String> allowedValues = table.asList(String.class);
                    assertTrue(allowedValues.stream().anyMatch(item -> values.get(0).equalsIgnoreCase(item)));
                });

        Then("^I should get any one of status code:$",
                (DataTable dataTable) -> {
                    List<Integer> statusCodes = dataTable.asList(Integer.class);
                    requestState.getResponseState().getResponse()
                            .then().statusCode(Matchers.isIn(statusCodes));
                });

        Then("^the response should have following tag values:$",
                (DataTable dataTable) -> {
                    Map<String, String> tagValues = dataTable.asMap(String.class, String.class);
                    for (Map.Entry<String, String> tag : tagValues.entrySet())
                        Assert.assertEquals(requestState.getResponseState().getResponse()
                                .jsonPath().get(tag.getKey()).toString(), paramTransformer.transform(tag.getValue()));
                });

        And("^I write Account \"([^\"]*)\" for customer \"([^\"]*)\" information$", (String acctId, String custId) -> {
            DocumentContext doc = JsonPath.parse(new File(System.getProperty("user.dir") + File.separator + "testdata.json")).set("$..Customer" + custId + ".Account" + acctId + ".SortCode", (String) requestState.getResponseState(paramTransformer.getCachedObject("log")).getValue(paramTransformer, "SortCode"));
            doc = doc.set("$..Customer" + custId + ".Account" + acctId + ".AccountNumber", (String) requestState.getResponseState(paramTransformer.getCachedObject("log")).getValue(paramTransformer, "AccountNumber"));
            doc = doc.set("$..Customer" + custId + ".Account" + acctId + ".AccountId", (String) requestState.getResponseState(paramTransformer.getCachedObject("log")).getValue(paramTransformer, "AccountId"));
            FileWriter writer = new FileWriter(System.getProperty("user.dir") + File.separator + "testdata.json");
            writer.write(doc.jsonString());
            writer.flush();
            acctId = acctId.replaceAll("\"", "");
        });

        And("^I download the PDF document as \"([^\"]*)\" in the response$",
                (String pdfDocName) -> {
                    helper.savePdfToFolder(pdfDocName);
                });

        And("^I validate the PDF document \"([^\"]*)\"$",
                (String pdfDocName) -> {
                    File file = new File(System.getProperty("user.dir") + "/PDF/" + pdfDocName + ".pdf");
                    doc = PDDocument.load(file);
                });

        And("^I check the PDF document has below text:$",
                (DataTable dataTable) -> {
                    String PDFContent = helper.getTextFromPDF(doc);
                    dataTable
                            .asList(String.class)
                            .forEach(
                                    text -> {
                                        assertThat(PDFContent, containsString((String) text));
                                    });
                });

        And("^I validate image is present in all the pages of the PDF document$", () -> {
            getImageLocationsAndSize.getImageLocAndSize(doc);
            Assert.assertEquals(imageLocAndSizeList.size(), pageCount);
        });

        And("^I validate image pixel size is (\\d+) (\\d+) in page (\\d+) of PDF document$",
                (Integer expectedImageWidth, Integer expectedImageHeight, Integer pageNumber) -> {
                    getImageLocationsAndSize.getImageLocAndSizeForAPage(doc, pageNumber);
                    Assert.assertEquals(expectedImageWidth.intValue(), ImageLocAndSize.getImageWidth());
                    Assert.assertEquals(expectedImageHeight.intValue(), ImageLocAndSize.getImageHeight());
                });
        And("^I validate image position is \"([^\"]*)\" \"([^\"]*)\" in page (\\d+) of PDF document$",
                (String expectedXImagePos, String expectedYImagePos, Integer pageNumber) -> {
                    getImageLocationsAndSize.getImageLocAndSizeForAPage(doc, pageNumber);
                    Assert.assertEquals(expectedXImagePos, String.valueOf(ImageLocAndSize.getPDFXImage()));
                    Assert.assertEquals(expectedYImagePos, String.valueOf(ImageLocAndSize.getPDFYImage()));
                });
        And("^I validate \"([^\"]*)\" is available in page (\\d+) of PDF document$",
                (String word, Integer pageCount) -> {
                    getWordsFromPDF.getWordsFromPdf(doc, word, pageCount);
                    Assert.assertNotNull(TextFondAndSize.getTextFont());
                });
        And("^I validate \"([^\"]*)\" is of font \"([^\"]*)\" in page (\\d+) of PDF document$",
                (String word, String expectedFont, Integer pageNumber) -> {
                    getWordsFromPDF.getWordsFromPdf(doc, word, pageNumber);
                    Assert.assertEquals(expectedFont, TextFondAndSize.getTextFont());
                });
        And("^I validate \"([^\"]*)\" is of font size \"([^\"]*)\" in page (\\d+) of PDF document$",
                (String word, String expectedFontSize, Integer pageNumber) -> {
                    getWordsFromPDF.getWordsFromPdf(doc, word, pageNumber);
                    Assert.assertEquals(expectedFontSize, String.valueOf(TextFondAndSize.getFontSize()));
                });
        And("^I validate \"([^\"]*)\" is of height \"([^\"]*)\" in page (\\d+) of PDF document$",
                (String word, String expectedHeight, Integer pageNumber) -> {
                    getWordsFromPDF.getWordsFromPdf(doc, word, pageNumber);
                    Assert.assertEquals(expectedHeight, String.valueOf(TextFondAndSize.getHeight()));
                });
        And("^I validate \"([^\"]*)\" is available in line (\\d+) page (\\d+) of PDF document$",
                (String word, Integer lineNumber, Integer pageNumber) -> {
                    getWordsFromPDF.getWordsFromPdf(doc, word, pageNumber);
                    Assert.assertTrue(lineNumberCheck.containsKey(lineNumber));
                });

    }

}
