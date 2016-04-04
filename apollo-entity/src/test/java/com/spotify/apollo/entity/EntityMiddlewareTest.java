/*
 * -\-\-
 * Spotify Apollo Entity Middleware
 * --
 * Copyright (C) 2013 - 2016 Spotify AB
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
package com.spotify.apollo.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.apollo.Environment;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Middleware;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.test.ServiceHelper;

import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.norberg.automatter.AutoMatter;
import io.norberg.automatter.jackson.AutoMatterModule;
import okio.ByteString;

import static com.spotify.apollo.entity.JsonMatchers.asStr;
import static com.spotify.apollo.entity.JsonMatchers.hasJsonPath;
import static com.spotify.apollo.test.unit.ResponseMatchers.doesNotHaveHeader;
import static com.spotify.apollo.test.unit.ResponseMatchers.hasHeader;
import static com.spotify.apollo.test.unit.ResponseMatchers.hasNoPayload;
import static com.spotify.apollo.test.unit.ResponseMatchers.hasPayload;
import static com.spotify.apollo.test.unit.ResponseMatchers.hasStatus;
import static com.spotify.apollo.test.unit.StatusTypeMatchers.withCode;
import static com.spotify.apollo.test.unit.StatusTypeMatchers.withReasonPhrase;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public final class EntityMiddlewareTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .registerModule(new AutoMatterModule());

  private static final ByteString PERSON_JSON = ByteString.encodeUtf8(
      "{\"name\":\"rouz\"}");

  private static final ByteString BAD_JSON = ByteString.encodeUtf8(
      "I am not Jayson");

  private static final String DIRECT = "/direct";
  private static final String DIRECT_O = "/direct-o";
  private static final String RESPONSE = "/response";
  private static final String RESPONSE_O = "/response-o";
  private static final String RESPONSE_E = "/response-e";
  private static final String GET_DIRECT = "/person";
  private static final String GET_RESPONSE = "/person-resp";
  private static final String UNSERIALIZABLE = "/unserializable";

  @ClassRule
  public static ServiceHelper serviceHelper =
      ServiceHelper.create(EntityMiddlewareTest::init, "entity-test");

  @Test
  public void testDirectPerson() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("POST", DIRECT, PERSON_JSON));

    assertThat(resp, hasStatus(withCode(Status.OK)));
    assertThat(resp, hasHeader("Content-Type", equalTo("application/json")));
    assertThat(resp, hasPayload(asStr(hasJsonPath("name", equalTo("hello rouz")))));
  }

  @Test
  public void testDirectOther() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("POST", DIRECT_O, PERSON_JSON));

    assertThat(resp, hasStatus(withCode(Status.OK)));
    assertThat(resp, hasHeader("Content-Type", equalTo("application/json")));
    assertThat(resp, hasPayload(asStr(hasJsonPath("people[0].name", equalTo("rouz")))));
  }

  @Test
  public void testResponse() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("POST", RESPONSE, PERSON_JSON));

    assertThat(resp, hasStatus(withCode(Status.ACCEPTED)));
    assertThat(resp, hasHeader("Content-Type", equalTo("application/json")));
    assertThat(resp, hasHeader("Name-Length", equalTo("4")));
    assertThat(resp, hasPayload(asStr(hasJsonPath("name", equalTo("rouz-resp")))));
  }

  @Test
  public void testResponseOther() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("POST", RESPONSE_O, PERSON_JSON));

    assertThat(resp, hasStatus(withCode(Status.ACCEPTED)));
    assertThat(resp, hasStatus(withReasonPhrase(equalTo("was rouz"))));
    assertThat(resp, hasHeader("Content-Type", equalTo("application/json")));
    assertThat(resp, hasPayload(asStr(hasJsonPath("people[0].name", equalTo("rouz")))));
  }

  @Test
  public void testResponseEmpty() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("POST", RESPONSE_E, PERSON_JSON));

    assertThat(resp, hasStatus(withCode(Status.ACCEPTED)));
    assertThat(resp, hasStatus(withReasonPhrase(equalTo("was rouz"))));
    assertThat(resp, doesNotHaveHeader("Content-Type"));
    assertThat(resp, hasNoPayload());
  }

  @Test
  public void testGetPerson() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("GET", GET_DIRECT));

    assertThat(resp, hasStatus(withCode(Status.OK)));
    assertThat(resp, hasHeader("Content-Type", equalTo("application/json")));
    assertThat(resp, hasPayload(asStr(hasJsonPath("name", equalTo("static")))));
  }

  @Test
  public void testGetPersonResponse() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("GET", GET_RESPONSE));

    assertThat(resp, hasStatus(withCode(Status.OK)));
    assertThat(resp, hasHeader("Content-Type", equalTo("application/json")));
    assertThat(resp, hasHeader("Reflection", equalTo("in-use")));
    assertThat(resp, hasPayload(asStr(hasJsonPath("name", equalTo("static-resp")))));
  }

  @Test
  public void testDirectAsync() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("POST", DIRECT + "-async", PERSON_JSON));

    assertThat(resp, hasStatus(withCode(Status.OK)));
    assertThat(resp, hasHeader("Content-Type", equalTo("application/json")));
    assertThat(resp, hasPayload(asStr(hasJsonPath("name", equalTo("hello rouz")))));
  }

  @Test
  public void testDirectAsyncOther() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("POST", DIRECT_O + "-async", PERSON_JSON));

    assertThat(resp, hasStatus(withCode(Status.OK)));
    assertThat(resp, hasHeader("Content-Type", equalTo("application/json")));
    assertThat(resp, hasPayload(asStr(hasJsonPath("people[0].name", equalTo("rouz")))));
  }

  @Test
  public void testResponseAsync() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("POST", RESPONSE + "-async", PERSON_JSON));

    assertThat(resp, hasStatus(withCode(Status.ACCEPTED)));
    assertThat(resp, hasHeader("Content-Type", equalTo("application/json")));
    assertThat(resp, hasHeader("Async", equalTo("true")));
    assertThat(resp, hasPayload(asStr(hasJsonPath("name", equalTo("hello rouz")))));
  }

  @Test
  public void testResponseAsyncOther() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("POST", RESPONSE_O + "-async", PERSON_JSON));

    assertThat(resp, hasStatus(withCode(Status.ACCEPTED)));
    assertThat(resp, hasHeader("Async", equalTo("true")));
    assertThat(resp, hasHeader("Content-Type", equalTo("application/json")));
    assertThat(resp, hasPayload(asStr(hasJsonPath("people[0].name", equalTo("rouz")))));
  }

  @Test
  public void testResponseAsyncEmpty() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("POST", RESPONSE_E + "-async", PERSON_JSON));

    assertThat(resp, hasStatus(withCode(Status.ACCEPTED)));
    assertThat(resp, hasHeader("Async", equalTo("true")));
    assertThat(resp, doesNotHaveHeader("Content-Type"));
    assertThat(resp, hasNoPayload());
  }

  @Test
  public void testGetAsyncPerson() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("GET", GET_DIRECT + "-async"));

    assertThat(resp, hasStatus(withCode(Status.OK)));
    assertThat(resp, hasHeader("Content-Type", equalTo("application/json")));
    assertThat(resp, hasPayload(asStr(hasJsonPath("name", equalTo("static-async")))));
  }

  @Test
  public void testGetAsyncPersonResponse() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("GET", GET_RESPONSE + "-async"));

    assertThat(resp, hasStatus(withCode(Status.OK)));
    assertThat(resp, hasHeader("Content-Type", equalTo("application/json")));
    assertThat(resp, hasHeader("Async", equalTo("true")));
    assertThat(resp, hasHeader("Reflection", equalTo("in-use")));
    assertThat(resp, hasPayload(asStr(hasJsonPath("name", equalTo("static-resp-async")))));
  }

  @Test
  public void testMissingPayloadDirect() throws Exception {
    Response<ByteString> resp = await(serviceHelper.request("POST", DIRECT));
    assertBadRequest(resp, equalTo("Missing payload"));
  }

  @Test
  public void testMissingPayloadDirectAsync() throws Exception {
    Response<ByteString> resp = await(serviceHelper.request("POST", DIRECT + "-async"));
    assertBadRequest(resp, equalTo("Missing payload"));
  }

  @Test
  public void testMissingPayloadResponse() throws Exception {
    Response<ByteString> resp = await(serviceHelper.request("POST", RESPONSE));
    assertBadRequest(resp, equalTo("Missing payload"));
  }

  @Test
  public void testMissingPayloadResponseAsync() throws Exception {
    Response<ByteString> resp = await(serviceHelper.request("POST", RESPONSE + "-async"));
    assertBadRequest(resp, equalTo("Missing payload"));
  }

  @Test
  public void testBadPayloadDirect() throws Exception {
    Response<ByteString> resp = await(serviceHelper.request("POST", DIRECT, BAD_JSON));
    assertBadRequest(resp, startsWith("Payload parsing failed"));
  }

  @Test
  public void testBadPayloadDirectAsync() throws Exception {
    Response<ByteString> resp = await(serviceHelper.request("POST", DIRECT + "-async", BAD_JSON));
    assertBadRequest(resp, startsWith("Payload parsing failed"));
  }

  @Test
  public void testBadPayloadResponse() throws Exception {
    Response<ByteString> resp = await(serviceHelper.request("POST", RESPONSE, BAD_JSON));
    assertBadRequest(resp, startsWith("Payload parsing failed"));
  }

  @Test
  public void testBadPayloadResponseAsync() throws Exception {
    Response<ByteString> resp = await(serviceHelper.request("POST", RESPONSE + "-async", BAD_JSON));
    assertBadRequest(resp, startsWith("Payload parsing failed"));
  }

  void assertBadRequest(Response<ByteString> resp, Matcher<String> reasonPhraseMatcher) {
    assertThat(resp, hasStatus(withCode(Status.BAD_REQUEST)));
    assertThat(resp, hasStatus(withReasonPhrase(reasonPhraseMatcher)));
    assertThat(resp, doesNotHaveHeader("Content-Type"));
    assertThat(resp, hasNoPayload());
  }

  @Test
  public void testBadResponse() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("GET", UNSERIALIZABLE));

    assertThat(resp, hasStatus(withCode(Status.INTERNAL_SERVER_ERROR)));
    assertThat(resp, hasStatus(withReasonPhrase(startsWith("Payload serialization failed"))));
    assertThat(resp, doesNotHaveHeader("Content-Type"));
    assertThat(resp, hasNoPayload());
  }

  @Test
  public void testBadResponseAsync() throws Exception {
    Response<ByteString> resp =
        await(serviceHelper.request("GET", UNSERIALIZABLE + "-async"));

    assertThat(resp, hasStatus(withCode(Status.INTERNAL_SERVER_ERROR)));
    assertThat(resp, hasStatus(withReasonPhrase(startsWith("Payload serialization failed"))));
    assertThat(resp, doesNotHaveHeader("Content-Type"));
    assertThat(resp, hasNoPayload());
  }

  /**
   * Entity based API used in tests
   */
  static void init(Environment environment) {
    EntityMiddlewareTest app = new EntityMiddlewareTest();
    EntityMiddleware e = EntityMiddleware.forJackson(OBJECT_MAPPER);

    Stream<Route<AsyncHandler<Response<ByteString>>>> syncRoutes = Stream.of(
        Route.with(e.direct(Person.class), "POST", DIRECT, rc -> app::personDirect),
        Route.with(e.direct(Person.class, Group.class), "POST", DIRECT_O, rc -> app::personOther),
        Route.with(e.response(Person.class), "POST", RESPONSE, rc -> app::personResponse),
        Route.with(e.response(Person.class, Group.class), "POST", RESPONSE_O, rc -> app::personOtherResponse),
        Route.with(e.response(Person.class, Void.class), "POST", RESPONSE_E, rc -> app::personEmptyResponse),
        Route.with(e.serializerDirect(Person.class), "GET", GET_DIRECT, rc -> app.getPerson()),
        Route.with(e.serializerResponse(Person.class), "GET", GET_RESPONSE, rc -> app.getPersonResponse()),
        Route.with(e.serializerDirect(Unserializable.class), "GET", UNSERIALIZABLE, rc -> app.getUnserializable())
    ).map(r -> r.withMiddleware(Middleware::syncToAsync));

    Stream<Route<AsyncHandler<Response<ByteString>>>> asyncRoutes = Stream.of(
        Route.with(e.asyncDirect(Person.class), "POST", DIRECT + "-async", rc -> app::personAsync),
        Route.with(e.asyncDirect(Person.class, Group.class), "POST", DIRECT_O + "-async", rc -> app::personAsyncOther),
        Route.with(e.asyncResponse(Person.class), "POST", RESPONSE + "-async", rc -> app::personAsyncResponse),
        Route.with(e.asyncResponse(Person.class, Group.class), "POST", RESPONSE_O + "-async", rc -> app::personAsyncOtherResponse),
        Route.with(e.asyncResponse(Person.class, Void.class), "POST", RESPONSE_E + "-async", rc -> app::personAsyncEmptyResponse),
        Route.with(e.asyncSerializerDirect(Person.class), "GET", GET_DIRECT + "-async", rc -> app.getPersonAsync()),
        Route.with(e.asyncSerializerResponse(Person.class), "GET", GET_RESPONSE + "-async", rc -> app.getPersonResponseAsync()),
        Route.with(e.asyncSerializerDirect(Unserializable.class), "GET", UNSERIALIZABLE + "-async", rc -> app.getUnserializableAsync())
    );

    environment.routingEngine()
        .registerRoutes(syncRoutes)
        .registerRoutes(asyncRoutes)
    ;
  }

  @AutoMatter
  public interface Person {
    String name();
  }

  @AutoMatter
  public interface Group {
    List<Person> people();
  }

  /**
   * Handler returning the entity type directly
   */
  Person personDirect(Person p) {
    return new PersonBuilder()
        .name("hello " + p.name())
        .build();
  }

  /**
   * Handler returning a some other entity directly
   */
  Group personOther(Person p) {
    return new GroupBuilder()
        .people(p)
        .build();
  }

  /**
   * Handler returning a response containing the entity type
   */
  Response<Person> personResponse(Person p) {
    Person p2 = PersonBuilder.from(p)
        .name(p.name() + "-resp")
        .build();

    return Response.of(Status.ACCEPTED, p2)
        .withHeader("Name-Length", Integer.toString(p.name().length()));
  }

  /**
   * Handler returning a response containing some other entity
   */
  Response<Group> personOtherResponse(Person p) {
    return Response.forStatus(Status.ACCEPTED.withReasonPhrase("was " + p.name()))
        .withPayload(new GroupBuilder()
                         .people(p)
                         .build());
  }

  /**
   * Handler returning a response with no entity
   */
  Response<Void> personEmptyResponse(Person p) {
    return Response.forStatus(Status.ACCEPTED.withReasonPhrase("was " + p.name()));
  }

  /**
   * Handler that only returns an entity
   */
  Person getPerson() {
    return new PersonBuilder()
        .name("static")
        .build();
  }

  /**
   * Handler that returns an entity in a response
   */
  Response<Person> getPersonResponse() {
    return Response.forPayload(
        new PersonBuilder()
            .name("static-resp")
            .build())
        .withHeader("Reflection", "in-use");
  }

  /**
   * Async handler returning an entity in a response
   */
  CompletionStage<Person> personAsync(Person p) {
    return completedFuture(personDirect(p));
  }

  /**
   * Async handler returning another entity in the response
   */
  CompletionStage<Group> personAsyncOther(Person p) {
    return completedFuture(personOther(p));
  }

  /**
   * Async handler returning an entity in a response
   */
  CompletionStage<Response<Person>> personAsyncResponse(Person p) {
    return completedFuture(
        Response.of(Status.ACCEPTED, personDirect(p))
            .withHeader("Async", "true"));
  }

  /**
   * Async handler returning a response with other entity
   */
  CompletionStage<Response<Group>> personAsyncOtherResponse(Person p) {
    return completedFuture(
        Response.of(Status.ACCEPTED, personOther(p))
            .withHeader("Async", "true"));
  }

  /**
   * Async handler returning a response with no entity
   */
  CompletionStage<Response<Void>> personAsyncEmptyResponse(Person p) {
    return completedFuture(
        Response.<Void>forStatus(Status.ACCEPTED)
            .withHeader("Async", "true"));
  }

  /**
   * Async handler that only returns an entity
   */
  CompletionStage<Person> getPersonAsync() {
    return completedFuture(
        new PersonBuilder()
            .name("static-async")
            .build());
  }

  /**
   * Async handler that returns an entity in a response
   */
  CompletionStage<Response<Person>> getPersonResponseAsync() {
    return completedFuture(
        Response.forPayload(
            new PersonBuilder()
                .name("static-resp-async")
                .build())
            .withHeader("Async", "true")
            .withHeader("Reflection", "in-use"));
  }

  /**
   * Handler returning an unserializable entity
   */
  Unserializable getUnserializable() {
    return new Unserializable() {};
  }

  /**
   * Async handler returning an unserializable entity
   */
  CompletionStage<Unserializable> getUnserializableAsync() {
    return completedFuture(new Unserializable() {});
  }

  static Response<ByteString> await(CompletionStage<Response<ByteString>> resp)
      throws InterruptedException, ExecutionException, TimeoutException {
    return resp.toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  interface Unserializable {
  }
}
