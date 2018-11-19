/*
 * -\-\-
 * Spotify Apollo Extra
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
package com.spotify.apollo.concurrent;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

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
                        },
                        directExecutor());

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
