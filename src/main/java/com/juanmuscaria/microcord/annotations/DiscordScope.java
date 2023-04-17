/*
 * Copyright 2023 juanmuscaria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.juanmuscaria.microcord.annotations;

import io.micronaut.runtime.context.scope.ScopedProxy;
import jakarta.inject.Scope;
import java.lang.annotation.*;

/**
 * A {@link io.micronaut.context.scope.CustomScope} that creates a new bean for every discord event.
 */
@ScopedProxy
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Scope
public @interface DiscordScope {}
