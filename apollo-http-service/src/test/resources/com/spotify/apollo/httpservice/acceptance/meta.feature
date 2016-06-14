@Focus
Feature: Metadata collection features

  Background:
    Given the "ping" service started in pod "my-pod" on port "5900"
    And a running reverser service
    And a request to "/reverse/petter" from "the acceptance test" has been completed

  Scenario: The service exposes general information
    When sending a request to "http://ping/_meta/0/info"
    Then the response contains "containerVersion"
    And the response contains "systemVersion"

  Scenario: The service exposes information about available endpoints
    When sending a request to "http://ping/_meta/0/endpoints"
    Then the response contains "Responds with pod and argument."
    And the response contains "/greet/<arg>"
    And the response contains "/reverse/<arg>"
    And the response contains "/uriencodingtest/<parameter>"

  Scenario: The service exposes information about outgoing calls
    When sending a request to "http://ping/_meta/0/calls"
    Then the response contains a call to the reverser service

  Scenario: The service exposes information about incoming calls
    When sending a request to "http://ping/_meta/0/calls"
    Then the response contains a call from "the acceptance test"
