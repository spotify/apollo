/*
 * -\-\-
 * Spotify Apollo API Interfaces
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
package com.spotify.apollo;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Indicates that a class is a part of an exploratory feature, meaning that it is a
 * feature whose implementation we don't feel has been validated in the field. It is fully
 * functional and will be supported to the same extent as other features in Apollo. We do,
 * however, feel that there is an above-average chance that we'll want to modify the API in a
 * breaking way in a future major release, or even remove or replace it. This also means that we
 * encourage users to provide feedback about aspects of this feature that work well or not.
 */
@Target({TYPE})
@Retention(SOURCE)
public @interface Exploratory {

}
