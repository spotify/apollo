Feature: Simple Apollo Standalone

  Scenario: Start with command line arguments and send a request
    Given the "ping" service started in pod "my-pod" on port "5900"
    When sending a request to "http://ping/greet/bar" on "tcp://localhost:5900"
    Then the response is "pod: my-pod, pong: bar"
    And application should have started in pod "my-pod"

  Scenario: Get the _meta api
    Given the "ping" service started in pod "dummy-pod" on port "5700"
    When sending a request to "http://ping/_meta/0/endpoints" on "tcp://localhost:5700"
    Then the response contains "Responds with pod and argument."
    And application should have started in pod "dummy-pod"

  Scenario: Proper encoding in URI
    Given the "ping" service started in pod "my-pod" on port "5900"
    When sending a request to "http://ping/uriencodingtest/%23test%23" on "tcp://localhost:5900"
    Then the response is "#test#"

  Scenario: Bad encoding in URI
    Given the "ping" service started in pod "my-pod" on port "5900"
    When sending a request to "http://ping/uriencodingtest/#test#" on "tcp://localhost:5900"
    Then the response code is 400
