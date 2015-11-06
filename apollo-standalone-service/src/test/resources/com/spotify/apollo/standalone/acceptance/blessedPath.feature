@Focus
Feature: Apollo Standalone Service with a service following the Blessed Path

  Background:
    Given the "ping" blessed-path service started in pod "my-pod" on port "5903"

  Scenario: synchronous endpoint handler via route provider
    When sending a request to "http://ping/route/bar" on "tcp://localhost:5903"
    Then the response is "RouteProvider:route bar"
    And the reason phrase is "this really is A-OK"

  Scenario: direct routing to synchronous endpoint handler
    When sending a request to "http://ping/route2/baz" on "tcp://localhost:5903"
    Then the response is "Direct:route baz"
    And the reason phrase is "this really is A-OK"

  Scenario: async endpoint via route provider
    When sending a request to "http://ping/async/bap" on "tcp://localhost:5903"
    Then the response is "RouteProvider:route bap"
    And the response has a header "X-MyHeader" with value "bap"

  Scenario: direct route to async endpoint
    When sending a request to "http://ping/async2/bad" on "tcp://localhost:5903"
    Then the response is "Direct:route bad"
    And the response has a header "X-MyHeader" with value "bad"
