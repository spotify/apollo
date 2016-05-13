Feature: Simple Apollo HTTP service

  Background:
    Given the "ping" service started in pod "my-pod" on port "5900"

  Scenario: Start with command line arguments and send a request
    When sending a request to "http://ping/greet/bar"
    Then the response is "pod: my-pod, pong: bar"
    And application should have started in pod "my-pod"

  Scenario: Proper encoding in URI
    When sending a request to "http://ping/uriencodingtest/%23test%23"
    Then the response is "#test#"

  Scenario: Bad encoding in URI
    When sending a request to "http://ping/uriencodingtest/#test#"
    Then the response code is 400
