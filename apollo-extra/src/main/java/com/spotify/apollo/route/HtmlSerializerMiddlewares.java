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
package com.spotify.apollo.route;

import com.google.common.base.Throwables;

import com.spotify.apollo.Response;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import java.io.StringWriter;

import okio.ByteString;

/**
 * A generic serializer for Object -> HTML using Freemarker templating.
 *
 * The templates are loaded from the "resources" folder with a base path of "/". If you put your template in
 * "resource/template/t.tmpl" then you have to use "template/t.tmpl" as the parameter.
 */
public class HtmlSerializerMiddlewares {

  private static final String CONTENT_TYPE = "Content-Type";
  private static final String HTML = "text/html; charset=UTF8";

  private static final Configuration configuration = new Configuration(Configuration.VERSION_2_3_22);

  static {
    configuration.setClassForTemplateLoading(HtmlSerializerMiddlewares.class, "/");
    configuration.setDefaultEncoding("UTF-8");
    configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    configuration.setIncompatibleImprovements(Configuration.VERSION_2_3_22);
  }

  private HtmlSerializerMiddlewares() {
  }

  /**
   * Call the template engine and return the result.
   *
   * @param templateName The template name, respective to the "resources" folder
   * @param object       The parameter to pass to the template
   * @param <T>          The Type of the parameters
   * @return The HTML
   */
  public static <T> ByteString serialize(final String templateName, T object) {
    StringWriter templateResults = new StringWriter();
    try {
      final Template template = configuration.getTemplate(templateName);
      template.process(object, templateResults);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
    return ByteString.encodeUtf8(templateResults.toString());
  }

  /**
   * Async middleware for POJO.
   *
   * @param templateName The template name, respective to the "resources" folder
   * @param <T>          The Type of the parameters
   * @return the middlware
   */
  public static <T> Middleware<AsyncHandler<T>, AsyncHandler<Response<ByteString>>> htmlSerialize(
      final String templateName) {
    return handler ->
        requestContext -> handler.invoke(requestContext)
            .thenApply(result -> Response
                .forPayload(serialize(templateName, result))
                .withHeader(CONTENT_TYPE, HTML));
  }

  /**
   * Async middleware for a Response object.
   *
   * @param templateName The template name, respective to the "resources" folder
   * @param <T>          The Type of the parameters
   * @return the middlware
   */
  public static <T> Middleware<AsyncHandler<Response<T>>, AsyncHandler<Response<ByteString>>>
  htmlSerializeResponse(final String templateName) {
    return handler ->
        requestContext -> handler.invoke(requestContext)
            .thenApply(response -> response
                .withPayload(serialize(templateName, response.payload().orElse(null)))
                .withHeader(CONTENT_TYPE, HTML));
  }

  /**
   * Sync middleware for POJO.
   *
   * @param templateName The template name, respective to the "resources" folder
   * @param <T>          The Type of the parameters
   * @return the middlware
   */
  public static <T> Middleware<SyncHandler<T>, AsyncHandler<Response<ByteString>>> htmlSerializeSync(
      final String templateName) {
    Middleware<SyncHandler<T>, AsyncHandler<T>> syncToAsync = Middleware::syncToAsync;
    return syncToAsync.and(htmlSerialize(templateName));
  }

  /**
   * Sync middleware for a Response object.
   *
   * @param templateName The template name, respective to the "resources" folder
   * @param <T>          The Type of the parameters
   * @return the middlware
   */
  public static <T> Middleware<SyncHandler<Response<T>>, AsyncHandler<Response<ByteString>>> htmlSerializeResponseSync(
      final String templateName) {
    Middleware<SyncHandler<Response<T>>, AsyncHandler<Response<T>>> syncToAsync = Middleware::syncToAsync;
    return syncToAsync.and(htmlSerializeResponse(templateName));
  }

}
