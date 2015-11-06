package com.spotify.apollo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class ResponseTest {

  @Test
  public void shouldHaveSingletonOK() throws Exception {
    Response<Object> ok1 = Response.ok();
    Response<Object> ok2 = Response.forStatus(Status.OK);

    assertSame(ok1, ok2);
  }

  @Test
  public void shouldNotIgnoreCustomOk() throws Exception {
    Response<Object> ok1 = Response.ok();
    Response<Object> ok2 = Response.forStatus(new CustomOK());

    assertNotSame(ok1, ok2);
  }

  static class CustomOK implements StatusType {

    @Override
    public int statusCode() {
      return 200;
    }

    @Override
    public String reasonPhrase() {
      return "Is more than OK";
    }

    @Override
    public Family family() {
      return Family.SUCCESSFUL;
    }

    @Override
    public StatusType withReasonPhrase(String reasonPhrase) {
      return this;
    }
  }

  @Test
  public void allowsOverrideHeaderValues() {
    Response<?> response = Response
        .forStatus(Status.OK)
        .withHeader("Content-Type", "application/json")
        .withHeader("Content-Type", "application/protobuf");

    assertEquals("application/protobuf", response.headers().get("Content-Type"));
  }
}
