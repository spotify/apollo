package com.spotify.apollo.http.common;

import static com.spotify.apollo.Status.IM_A_TEAPOT;

import com.google.common.collect.Lists;
import com.spotify.apollo.Response;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.RequestHandler;
import java.util.List;

public class TestHandler implements RequestHandler {

    List<OngoingRequest> requests = Lists.newLinkedList();

    @Override
    public void handle(OngoingRequest request) {
      requests.add(request);
      request.reply(Response.forStatus(IM_A_TEAPOT));
    }

  public List<OngoingRequest> getRequests() {
    return requests;
  }
}