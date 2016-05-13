/*
 * -\-\-
 * Spotify Apollo HTTP Service
 * --
 * Copyright (C) 2013 - 2015 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */
package com.spotify.apollo.httpservice.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import okio.ByteString;

import static com.spotify.apollo.Status.OK;
import static com.spotify.apollo.test.unit.ResponseMatchers.hasStatus;
import static com.spotify.apollo.test.unit.StatusTypeMatchers.withCode;
import static com.spotify.apollo.test.unit.StatusTypeMatchers.withReasonPhrase;
import static java.util.concurrent.TimeUnit.SECONDS;
import static okio.ByteString.encodeUtf8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class RequestStepdefs {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  CompletionStage<Response<ByteString>> responseFuture;

  @When("^sending a request to \"([^\"]*)\"$")
  public void sending_a_request_to(String uri) throws Throwable {
    Request request = Request.forUri(uri);

    responseFuture = ServiceStepdefs.serviceHelper.request(request);
  }

  @Then("^the response is \"([^\"]*)\"$")
  public void the_response_is(String expected) throws Throwable {
    Response<ByteString> response = getResponseFuture();
    assertThat(response.status(), withCode(OK));

    String actual = response.payload().get().utf8();

    assertEquals(expected, actual);
  }

  private Response<ByteString> getResponseFuture()
      throws InterruptedException, ExecutionException, TimeoutException {
    return this.responseFuture.toCompletableFuture().get(5, SECONDS);
  }

  @And("^the response should be written down to \"([^\"]*)\"$")
  public void the_response_is_written(String dir) throws Throwable {
    // check that we have at least one file in
    File dirFile = new File(dir);
    File[] files = dirFile.listFiles();
    assertNotNull(files);
    assertEquals(1, files.length);
  }

  @Then("^the response contains \"([^\"]*)\"$")
  public void the_response_contains(String needle) throws Throwable {
    Response<ByteString> response = getResponseFuture();
    String reply = response.payload().get().utf8();

    assertTrue(reply.contains(needle));
  }

  @Then("^the response code is (\\d+)$")
  public void the_response_code_is(int statusCode) throws Throwable {
    Response<ByteString> response = getResponseFuture();
    assertThat(response.status(), withCode(statusCode));
  }

  @And("^the response has a header \"([^\"]*)\" with value \"([^\"]*)\"$")
  public void the_response_has_a_header_with_value(String headerName, String expectedValue) throws Throwable {
    Response<ByteString> response = getResponseFuture();

    assertThat(response.headers().get(headerName), equalTo(Optional.of(expectedValue)));
  }

  @And("^the reason phrase is \"([^\"]*)\"$")
  public void the_reason_phrase_is(String expected) throws Throwable {
    Response<ByteString> response = getResponseFuture();

    assertThat(response.status(), withReasonPhrase(is(expected)));
  }

  @And("^requests to \"([^\"]*)\" lead to a response with payload \"([^\"]*)\"$")
  public void requests_to_are_responded_to_with(String remoteUri, String responsePayload) throws Throwable {
    ServiceStepdefs.serviceHelper.stubClient()
        .respond(Response.forPayload(encodeUtf8(responsePayload)))
        .to(remoteUri);
  }

  @And("^a request to \"([^\"]*)\" from \"([^\"]*)\" has been completed$")
  public void aRequestToFromHasBeenCompleted(String uri, String callingService) throws Throwable {
    Request request = Request.forUri(uri).withService(callingService);

    assertThat(ServiceStepdefs.serviceHelper.request(request).toCompletableFuture().get(),
               hasStatus(withCode(OK)));
  }

  @Then("^the response contains a call to the reverser service$")
  public void theResponseContainsACallToTheReverserService() throws Throwable {
    Response<ByteString> response = getResponseFuture();

    assertThat(response, hasStatus(withCode(OK)));

    JsonNode jsonNode = OBJECT_MAPPER.readTree(response.payload().get().utf8());
    JsonNode outgoingCallsNode = jsonNode.get("result").get("outgoing");

    assertThat(outgoingCallsNode.has(AcceptanceIT.REVERSER_ADDRESS.substring("http://".length())),
               is(true));
  }

  @Then("^the response contains a call from \"([^\"]*)\"$")
  public void theResponseContainsACallFrom(String callingService) throws Throwable {
    Response<ByteString> response = getResponseFuture();

    assertThat(response, hasStatus(withCode(OK)));

    JsonNode jsonNode = OBJECT_MAPPER.readTree(response.payload().get().utf8());
    JsonNode incomingCallsNode = jsonNode.get("result").get("incoming");

    assertThat(incomingCallsNode.has(callingService), is(true));
  }
}
