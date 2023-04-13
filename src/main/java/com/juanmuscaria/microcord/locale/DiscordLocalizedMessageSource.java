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

import com.juanmuscaria.microcord.annotations.DiscordScope;
import com.juanmuscaria.microcord.scope.DiscordAware;
import com.juanmuscaria.microcord.scope.DiscordContextData;
import io.micronaut.context.AbstractLocalizedMessageSource;
import io.micronaut.context.MessageSource;
import io.micronaut.core.util.LocaleResolver;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;

@DiscordScope
public class DiscordLocalizedMessageSource
    extends AbstractLocalizedMessageSource<DiscordContextData> implements DiscordAware {
  private Locale locale;

  /**
   * @param localeResolver The locale resolver
   * @param messageSource The message source
   */
  public DiscordLocalizedMessageSource(
      LocaleResolver<DiscordContextData> localeResolver, MessageSource messageSource) {
    super(localeResolver, messageSource);
  }

  @NotNull @Override
  protected Locale getLocale() {
    if (locale == null) {
      throw new IllegalStateException("DiscordAware::setDiscordContext should have set the locale");
    }
    return locale;
  }

  @Override
  public void setDiscordContext(DiscordContextData context) {
    this.locale = resolveLocale(context);
  }
}
