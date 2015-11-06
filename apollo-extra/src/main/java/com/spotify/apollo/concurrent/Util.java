package com.spotify.apollo.concurrent;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Contains some utilities for working with futures and completion stages.
 */
public final class Util {
  private Util() {
    // prevent instantiation
  }

  public static <T> CompletionStage<T> asStage(ListenableFuture<T> future) {
    CompletableFuture<T> completableFuture = new CompletableFuture<>();

    Futures.addCallback(future,
                        new FutureCallback<T>() {
                          @Override
                          public void onSuccess(T result) {
                            completableFuture.complete(result);
                          }

                          @Override
                          public void onFailure(Throwable t) {
                            completableFuture.completeExceptionally(t);
                          }
                        });

    return completableFuture;
  }

  public static <T> ListenableFuture<T> asFuture(CompletionStage<T> stage) {
    SettableFuture<T> future = SettableFuture.create();

    stage.whenComplete((result, throwable) -> {
                         if (throwable != null) {
                           future.setException(throwable);
                         } else {
                           future.set(result);
                         }
                       });

      return future;
    }
  }
