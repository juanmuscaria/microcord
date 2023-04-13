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
package com.juanmuscaria.microcord.scope;

import com.juanmuscaria.microcord.annotations.DiscordScope;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.scope.AbstractConcurrentCustomScope;
import io.micronaut.context.scope.BeanCreationContext;
import io.micronaut.context.scope.CreatedBean;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.BeanIdentifier;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Discord custom scope largely based on HttpRequest custom scope */
@Singleton
public class DiscordContextCustomScope extends AbstractConcurrentCustomScope<DiscordScope>
    implements ApplicationEventListener<DiscordContextTerminatedEvent> {

  public static final String SCOPED_BEANS_ATTRIBUTE = "com.juanmuscaria.microcord.SCOPED_BEANS";

  public DiscordContextCustomScope() {
    super(DiscordScope.class);
  }

  @Override
  public void close() {
    DiscordContext.currentContext().ifPresent(this::destroyBeans);
  }

  @Override
  public boolean isRunning() {
    return DiscordContext.currentContext().isPresent();
  }

  @Override
  public void onApplicationEvent(DiscordContextTerminatedEvent event) {
    destroyBeans(event.getSource());
  }

  @NonNull @Override
  protected Map<BeanIdentifier, CreatedBean<?>> getScopeMap(boolean forCreation) {
    final DiscordContextData context = DiscordContext.currentContext().orElse(null);
    if (context != null) {
      //noinspection ConstantConditions
      return getContextAttributeMap(context, forCreation);
    } else {
      throw new IllegalStateException("No context present");
    }
  }

  @NonNull @Override
  protected <T> CreatedBean<T> doCreate(@NonNull BeanCreationContext<T> creationContext) {
    final DiscordContextData context = DiscordContext.currentContext().orElse(null);
    final CreatedBean<T> createdBean = super.doCreate(creationContext);
    final T bean = createdBean.bean();
    if (bean instanceof DiscordAware discordAware) {
      discordAware.setDiscordContext(context);
    }
    return createdBean;
  }

  private void destroyBeans(DiscordContextData context) {
    ConcurrentHashMap<BeanIdentifier, CreatedBean<?>> requestScopedBeans =
        getContextAttributeMap(context, false);
    if (requestScopedBeans != null) {
      destroyScope(requestScopedBeans);
    }
  }

  @SuppressWarnings("unchecked")
  private ConcurrentHashMap<BeanIdentifier, CreatedBean<?>> getContextAttributeMap(
      DiscordContextData context, boolean create) {
    Map<String, Object> attrs = context.getAttributes();
    Object o = attrs.get(SCOPED_BEANS_ATTRIBUTE);
    if (o instanceof ConcurrentHashMap) {
      return (ConcurrentHashMap<BeanIdentifier, CreatedBean<?>>) o;
    }
    if (create) {
      ConcurrentHashMap<BeanIdentifier, CreatedBean<?>> scopedBeans = new ConcurrentHashMap<>();
      attrs.put(SCOPED_BEANS_ATTRIBUTE, scopedBeans);
      return scopedBeans;
    }
    return null;
  }
}
