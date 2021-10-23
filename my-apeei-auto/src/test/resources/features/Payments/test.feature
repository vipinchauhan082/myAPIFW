@test
Feature: Sample HMRC Test

  Scenario:
    Given I generate auth token
    And  I validate auth token
    And record the response as "authResponse"
    Given I am a JSON API consumer
    And I provide the header "Authorization" with a value of "{{response::authResponse->token}}"
    And I provide the header "IB-ms-app-nonce" with a value of "{{response.headers::authResponse->IB-ms-app-nonce}}"
    And I remove the header "x-asd-channel"
    When I request GET for "getTestAccountList" from "getTestAccountList-baseuri"
    Then I should get a status code of 200


