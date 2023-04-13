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
package com.juanmuscaria.microcord.locale;

import com.juanmuscaria.microcord.scope.DiscordContextData;
import io.micronaut.core.util.locale.AbstractLocaleResolver;
import jakarta.inject.Singleton;
import java.util.Locale;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

@Singleton
public class DiscordLocaleResolver extends AbstractLocaleResolver<DiscordContextData> {

  public DiscordLocaleResolver() {
    super(Locale.ENGLISH);
  }

  @NotNull @Override
  public Optional<Locale> resolve(DiscordContextData context) {
    var locale = context.getUserLocale();
    locale = locale == null ? context.getGuildLocale() : locale;
    if (locale == null) {
      return Optional.empty();
    } else {
      return Optional.of(Locale.forLanguageTag(locale.getLocale()));
    }
  }
}
