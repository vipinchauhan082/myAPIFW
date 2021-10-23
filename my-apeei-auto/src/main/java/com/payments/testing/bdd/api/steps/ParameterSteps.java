package com.payments.testing.bdd.api.steps;

import com.payments.testing.bdd.api.http.APIRequestState;
import com.payments.testing.bdd.parameters.DefaultParamTransformer;
import com.payments.testing.bdd.util.BDDConfig;
import com.payments.testing.bdd.util.Helper;
import com.payments.testing.bdd.util.YamlReader;
import com.typesafe.config.Config;
import cucumber.api.java8.En;
import io.cucumber.datatable.DataTable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParameterSteps implements En {
    private static Config config = BDDConfig.getConfig();

    /**
     * container for cucumber lambda methods.
     *
     * @param requestState     injected object
     * @param paramTransformer injected object
     */
    public ParameterSteps(APIRequestState requestState, DefaultParamTransformer paramTransformer) {
        Helper helper = new Helper(requestState, paramTransformer);

        /**
         * add a parameter to the request.
         *
         * @param name the parameter name
         * @param val the paramater value
         */
        When(
                "I provide the parameter {string} with a value of {string}",
                (String name, String val) -> {
                    helper.setQueryParam(name, val);
                });

        /**
         * adds parameters to request.
         *
         * @param table the parameters to add
         */
        When(
                "^I provide the parameters$",
                (DataTable table) -> {
                    helper.setQueryParams(table);
                });

        When("^I set the parameter \"([^\"]*)\" for \"([^\"]*)\" from \"([^\"]*)\"$",
                (String queryParam, String file, String value) -> {
                    helper.setQueryParamFromFile(queryParam, file, value);
                });
        When("^I set the path parameter \"([^\"]*)\" as \"([^\"]*)\"$",
                (String pathParam, String value) -> {
                    helper.setPathParam(pathParam, value);
                });
        When("^I set the path parameter \"([^\"]*)\" for \"([^\"]*)\" from \"([^\"]*)\"$",
                (String pathParam, String file, String value) -> {
                    helper.setPathParamFromFile(pathParam, file, value);
                });
        When("^I set the form parameters for \"([^\"]*)\" from \"([^\"]*)\" yml$",
                (String ymlFile, String data) -> {
                    helper.setFormParamsFromYml(ymlFile, data);
                });

    }
}
