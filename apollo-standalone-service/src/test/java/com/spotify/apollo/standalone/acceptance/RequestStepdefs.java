/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.standalone.acceptance;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;

import java.io.File;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import okio.ByteString;

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

  CompletionStage<Response<ByteString>> responseFuture;

  @When("^sending a request to \"([^\"]*)\" on \"([^\"]*)\"$")
  public void sending_a_request_to(String uri, String endpoint) throws Throwable {
    Request request = Request.forUri(uri);

    responseFuture = ServiceStepdefs.serviceHelper.request(request);
  }

  @Then("^the response is \"([^\"]*)\"$")
  public void the_response_is(String expected) throws Throwable {
    Response<ByteString> response = getResponseFuture();
    assertThat(response.status(), withCode(Status.OK));

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

    assertThat(response.headers().get(headerName), equalTo(expectedValue));
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
}
