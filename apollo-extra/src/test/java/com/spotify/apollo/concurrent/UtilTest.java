package com.spotify.apollo.concurrent;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.spotify.test.Util.hasAncestor;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class UtilTest {

  SettableFuture<String> future;
  CompletableFuture<Integer> stage;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    future = SettableFuture.create();
    stage = new CompletableFuture<>();
  }

  @Test
  public void shouldConvertToStage() throws Exception {
    CompletionStage<String> converted = Util.asStage(future);

    future.set("i'm done!");

    assertThat(converted.toCompletableFuture().get(), equalTo("i'm done!"));
  }

  @Test
  public void shouldPropagateExceptionsToStage() throws Exception {
    CompletionStage<String> converted = Util.asStage(future)
        .exceptionally(Throwable::getMessage);

    NullPointerException exception = new NullPointerException("expected exception");

    future.setException(exception);

    assertThat(converted.toCompletableFuture().get(), equalTo("expected exception"));
  }

  @Test
  public void shouldConvertToFuture() throws Exception {
    ListenableFuture<Integer> converted = Util.asFuture(stage);

    stage.complete(9834);

    assertThat(converted.get(), equalTo(9834));
  }

  @Test
  public void shouldHandleNullsFromStage() throws Exception {
    ListenableFuture<Integer> converted = Util.asFuture(stage);

    stage.complete(null);

    assertThat(converted.get(), is(nullValue()));
  }

  @Test
  public void shouldPropagateExceptionsToFuture() throws Exception {
    ListenableFuture<Integer> converted = Util.asFuture(stage);

    NullPointerException expected = new NullPointerException("expected");
    stage.completeExceptionally(expected);

    thrown.expect(hasAncestor(expected));
    converted.get();
  }
}