package com.payments.testing.bdd.api.steps;

import com.payments.testing.bdd.api.http.APIRequestState;
import com.payments.testing.bdd.comparison.Matcher;
import com.payments.testing.bdd.parameters.DefaultParamTransformer;
import com.payments.testing.bdd.util.BDDConfig;
import com.payments.testing.bdd.util.Helper;
import com.payments.testing.bdd.util.YamlReader;
import com.typesafe.config.Config;
import cucumber.api.java8.En;
import io.cucumber.datatable.DataTable;
import io.cucumber.datatable.dependency.com.fasterxml.jackson.core.type.TypeReference;
import io.cucumber.datatable.dependency.com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.dependency.com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.util.Pair;
import org.junit.Assert;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.io.*;

public class HeaderSteps implements En {

    private static Config config = BDDConfig.getConfig();

    /**
     * container for cucumber lambda methods.
     *
     * @param requestState     injected object
     * @param paramTransformer injected object
     */
    public HeaderSteps(APIRequestState requestState, DefaultParamTransformer paramTransformer) {
        Helper helper = new Helper(requestState, paramTransformer);

        /**
         * add a header to the request.
         *
         * @param name the header name
         * @param val the header value
         */
        When(
                "I provide the header {string} with a value of {string}",
                (String name, String val) -> {
                    String paramVal = paramTransformer.transform(val);
                    requestState.setHeader(name, paramVal);
                });

        /**
         * remove a header to the request.
         *
         * @param name the header name

         */
        When(
                "I remove the header {string}",
                (String name) -> {
                    requestState.removeHeader(name);
                });

        /**
         * adds a bunch of headers to the request.
         *
         * @param table the headers to add
         */
        When(
                "^I provide the headers$",
                (DataTable table) -> {
                    helper.setHeaders(table);
                });

        /**
         * header "name" equals "value".
         *
         * @param name the header name
         * @param val the header value
         */
        Then(
                "match header named {string} with a value of {string}",
                (String name, String val) -> {
                    name = paramTransformer.transform(name);
                    val = paramTransformer.transform(val);
                    String hv =
                            Optional.of(requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().getHeader(name))
                                    .orElse("<EMPTY>");
                    Assert.assertTrue(
                            String.format("Expected %s to be %s was %s", name, val, hv), Objects.equals(hv, val));
                });

        /**
         * header value matching as a datatable.
         *
         * @param table the headers to match
         */
        When(
                "^the following header name/value combinations are (equal|not equal)$",
                (String op, DataTable table) -> {
                    final AtomicReference<String> operation = new AtomicReference<>(op.replace(' ', '_'));
                    Map<String, String> pairs = table.asMap(String.class, String.class);
                    pairs.forEach((name, val) -> {
                        name = paramTransformer.transform((String) name);
                        val = paramTransformer.transform((String) val);
                        String hv =
                                Optional.of(requestState.getResponseState(paramTransformer.getCachedObject("log")).getResponse().getHeader(name))
                                        .orElse("<EMPTY>");
                        Assert.assertTrue(
                                String.format("Expected %s to be %s was %s", name, val, hv), Matcher.valueOf(operation.get()).match(val, hv));
                    });
                });

        When("^I set the headers for \"([^\"]*)\" from \"([^\"]*)\"$",
                (String schema, String testData) -> {
                    helper.setHeaders(schema, testData);
                });

        When("^I set the headers for \"([^\"]*)\"$",
                (String schema) -> {
                    helper.setHeaderFromJson(schema);
                });

        When("^I set the headers for \"([^\"]*)\" from \"([^\"]*)\" yml$",
                (String ymlFile, String data) -> {
                    helper.setHeaderFromYml(ymlFile, data);
                });
    }
}
