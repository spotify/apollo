/*
 * -\-\-
 * Spotify Apollo Testing Helpers
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
package com.spotify.apollo.test;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.module.ApolloModule;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;

public final class ServiceHelperExtension
    implements ServerHelperSetup<ServiceHelperExtension>,
    BeforeEachCallback,
    AfterEachCallback,
    ParameterResolver {

  private final ServiceHelper serviceHelper;

  private ServiceHelperExtension() {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not support declarative registration via @"
            + ExtendWith.class.getSimpleName()
            + ". Please declare this extension programmatically as a field using @"
            + RegisterExtension.class.getSimpleName());
  }

  private ServiceHelperExtension(ServiceHelper serviceHelper) {
    this.serviceHelper = serviceHelper;
  }

  public static ServiceHelperExtension create(AppInit appInit, String serviceName) {
    return new ServiceHelperExtension(ServiceHelper.create(appInit, serviceName));
  }

  public static ServiceHelperExtension create(
      AppInit appInit, String serviceName, StubClient stubClient) {
    return new ServiceHelperExtension(ServiceHelper.create(appInit, serviceName, stubClient));
  }

  @Override
  public ServiceHelperExtension domain(String domain) {
    serviceHelper.domain(domain);
    return this;
  }

  @Override
  public ServiceHelperExtension disableMetaApi() {
    serviceHelper.disableMetaApi();
    return this;
  }

  @Override
  public ServiceHelperExtension args(String... args) {
    serviceHelper.args();
    return this;
  }

  @Override
  public ServiceHelperExtension conf(String key, String value) {
    serviceHelper.conf(key, value);
    return this;
  }

  @Override
  public ServiceHelperExtension conf(String key, Object value) {
    serviceHelper.conf(key, key);
    return this;
  }

  @Override
  public ServiceHelperExtension resetConf(String key) {
    serviceHelper.resetConf(key);
    return this;
  }

  @Override
  public ServiceHelperExtension forwardingNonStubbedRequests(boolean forward) {
    serviceHelper.forwardingNonStubbedRequests(forward);
    return this;
  }

  @Override
  public ServiceHelperExtension startTimeoutSeconds(int timeoutSeconds) {
    serviceHelper.startTimeoutSeconds(timeoutSeconds);
    return this;
  }

  @Override
  public ServiceHelperExtension withModule(ApolloModule module) {
    serviceHelper.withModule(module);
    return this;
  }

  @Override
  public ServiceHelperExtension scheme(String scheme) {
    serviceHelper.scheme(scheme);
    return this;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    serviceHelper.start();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    serviceHelper.close();
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context)
      throws ParameterResolutionException {
    return ServiceHelper.class.equals(parameterContext.getParameter().getType());
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context)
      throws ParameterResolutionException {
    return serviceHelper;
  }

  public ServiceHelper getServiceHelper() {
    return serviceHelper;
  }
}
