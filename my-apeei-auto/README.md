# microservice-test
Test Automation repository for Microservice Testing


## Running tests
-----------------------------------------------------

### To run tests 
Go to your project directory from terminal and execute following commands
* `mvn clean test` to run all the tests

##### Run scenario with 1 tag
* `mvn test  -Dcucumber.options="--tags '@getaccount'"`
* `mvn test  -Dcucumber.options="--tags '@scenario1'"`
* `mvn test  -Dcucumber.options="--tags '@transactions'"`
* `mvn test  -Dcucumber.options="--tags '@regression'"`

##### Run scenario with tag1 OR tag2
* `mvn test  -Dcucumber.options="--tags '@accounts or @transactions'"`

##### Run scenario with tag1 AND tag2
* ` mvn test  -Dcucumber.options="--tags '@accounts and @transactions'"`

##### Rerun scenario with tag:
* `mvn test  -Dcucumber.options="--tags '@regression or @smoke'"`

##### Ignore a tag and rerun failed scenario:
* `mvn test  -Dcucumber.options="--tags '@regression and not @ignore' --plugin rerun:rerun/failed_scenarios.txt"`


### Running test on different profiles

##### Local execution on Build2 ACT
* `mvn test -Plocal`

##### Local execution on Build2 MFA 
* `mvn test -Plocalmfa`

##### Default Profile i.e. Build2 ACT with Zap
* `mvn test`


### Microservice level execution

#### All API scenarios:
`mvn clean test -Plocalmfa -Dcucumber.options="--tags '@api and not @ignore and not @bug and not @wip and not @manual'"`

#### Accounts scenarios:
`mvn clean test -Plocalmfa -Dcucumber.options="--tags '@accounts and not @ignore and not @bug and not @wip and not @manual'"`

#### Balances scenarios:
`mvn clean test -Plocalmfa -Dcucumber.options="--tags '@balances and not @ignore and not @bug and not @wip and not @manual'"`

#### Products scenarios:
`mvn clean test -Plocalmfa -Dcucumber.options="--tags '@products and not @ignore and not @bug and not @wip and not @manual'"`

#### Transaction scenarios:
`mvn clean test -Plocalmfa -Dcucumber.options="--tags '@transactions and not @ignore and not @bug and not @wip and not @manual'"`

#### Entitlements scenarios:
`mvn clean test -Plocalmfa -Dcucumber.options="--tags '@entitlements and not @ignore and not @bug and not @wip and not @manual'"`

#### INT/E2E scenarios:
`mvn clean test -Pintegration -Dcucumber.options="--tags '@AccountsE2E and not @ignore and not @bug and not @wip and not @manual'"`

## Generate Report( Allure is used for default Reporting)
--------------------------------------------------

Run below command for local:
* Run `allure generate target/allure-results --clean && allure open`

Run below command for Jenkins:
1. First run: `allure generate target/allure-results --clean`
2. Then run `allure open`


## To Run Spanner scenarios locally (mvn test -Plocal)
--------------------------------------------------
1. Install Google Cloud SDK:
   * For MAC: https://cloud.google.com/sdk/docs/quickstart-macos
   * For Windows: https://cloud.google.com/sdk/docs/quickstart-windows
2. Run the command to authenticate credentials: `gcloud auth application-default login`
3. Then add the .json file generated as environment variable
    * For MAC: `export GOOGLE_APPLICATION_CREDENTIALS=“path to credentials.json`
    * For Windows: `set GOOGLE_APPLICATION_CREDENTIALS=“path to credentials.json`
4. If in case you have logged out and spanner is not connected, 
authenticate again using cmd: `gcloud auth application-default login`

## Maven BDD Base image:
Images:
harbor.mgmt-bld.oncp.dev/application_tools_rtl/mvn-bdd-base:release_v0.42.0
eu.gcr.io/eplus-mfa-bld-02-1727/application_tools/mvn-bdd-base:release_v0.42.0
GCR RTL: eu.gcr.io/mgmt-bak-prd-dbdf/application_tools_rtl/mvn-bdd-base:release_v0.41.0

Aqua:
GCR: https://aqua.mgmt-bld.oncp.dev/#/images/Ad%20Hoc%20Scans/harbor.mgmt-bld.oncp.dev%2Fapplication_tools_rtl%2Fmvn-bdd-base:release_v0.42.0
HARBOR: https://aqua.mgmt-bld.oncp.dev/#/images/Ad%20Hoc%20Scans/eu.gcr.io%2Feplus-mfa-bld-02-1727%2Fapplication_tools%2Fmvn-bdd-base:release_v0.42.0
