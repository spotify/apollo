package com.spotify.apollo.example;

import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.StatusType;
import com.spotify.apollo.test.ServiceHelper;
import com.spotify.apollo.test.StubClient;

import org.junit.Rule;
import org.junit.Test;

import okio.ByteString;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MinimalAppTest {

  private static final String BREWERY_ORDER_URI = "http://brewery/order";
  private static final String ORDER_REPLY = "order0443";
  private static final ByteString ORDER_REPLY_BYTES = ByteString.encodeUtf8(ORDER_REPLY);

  @Rule
  public ServiceHelper serviceHelper = ServiceHelper.create(MinimalApp::init, "test");

  public StubClient stubClient= serviceHelper.stubClient();

  @Test
  public void shouldRespondWithOrderId() throws Exception {
    stubClient.respond(Response.forPayload(ORDER_REPLY_BYTES)).to(BREWERY_ORDER_URI);

    String reply = serviceHelper.request("GET", "/beer")
        .toCompletableFuture().get()
        .payload().get().utf8();

    assertThat(reply, is("your order is " + ORDER_REPLY));
  }

  @Test
  public void shouldRespondWithStatusCode() throws Exception {
    stubClient.respond(Response.of(Status.IM_A_TEAPOT, ORDER_REPLY_BYTES)).to(BREWERY_ORDER_URI);

    StatusType status = serviceHelper.request("GET", "/beer")
        .toCompletableFuture().get()
        .status();

    assertThat(status.code(), is(Status.INTERNAL_SERVER_ERROR.code()));
  }

  @Test
  public void shouldBeOkWithPayload() throws Exception {
    stubClient.respond(Response.of(Status.IM_A_TEAPOT, ORDER_REPLY_BYTES)).to(BREWERY_ORDER_URI);

    StatusType status = serviceHelper
        .request("POST", "/beer", ByteString.encodeUtf8("{\"key\": \"value\"}"))
        .toCompletableFuture().get()
        .status();

    assertThat(status.code(), is(Status.INTERNAL_SERVER_ERROR.code()));
  }
}
