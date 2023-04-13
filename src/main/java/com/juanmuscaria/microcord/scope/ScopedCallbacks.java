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

import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Singleton;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Work around the current discord scope being lost in JDA callbacks, wrap callbacks that requires
 * the current scope.
 */
// TODO: Create a custom JDA implementation and hook into callbacks directly to make a seamless
// context transitions
@SuppressWarnings("unused")
@AllArgsConstructor
@Singleton
public class ScopedCallbacks {
  private static final Logger logger = LoggerFactory.getLogger(ScopedCallbacks.class);
  private final ApplicationEventPublisher<DiscordContextTerminatedEvent> publisher;

  /**
   * Wraps a callable in the current DiscordContext
   *
   * @param original - original callable
   * @return a wrapped callable in the current context, if there's no context the original callable
   *     will be returned
   */
  public <T> Callable<T> wrap(Callable<T> original) {
    if (DiscordContext.currentContext().isPresent()) {
      var context = DiscordContext.currentContext().get().copy();
      return () -> {
        var prev = DiscordContext.currentContext().orElse(null);
        DiscordContext.set(context);
        try {
          return original.call();
        } finally {
          try {
            publisher.publishEvent(new DiscordContextTerminatedEvent(context));
          } catch (Throwable e) {
            logger.error("An error occurred while terminating event context", e);
          }
          DiscordContext.set(prev);
        }
      };
    } else {
      return original;
    }
  }

  /**
   * Wraps a supplier in the current DiscordContext
   *
   * @param original - original supplier
   * @return a wrapped supplier in the current context, if there's no context the original supplier
   *     will be returned
   */
  public <T> Supplier<T> wrap(Supplier<T> original) {
    if (DiscordContext.currentContext().isPresent()) {
      var context = DiscordContext.currentContext().get().copy();
      return () -> {
        var prev = DiscordContext.currentContext().orElse(null);
        DiscordContext.set(context);
        try {
          return original.get();
        } finally {
          try {
            publisher.publishEvent(new DiscordContextTerminatedEvent(context));
          } catch (Throwable e) {
            logger.error("An error occurred while terminating event context", e);
          }
          DiscordContext.set(prev);
        }
      };
    } else {
      return original;
    }
  }

  /**
   * Wraps a runnable in the current DiscordContext
   *
   * @param original - original runnable
   * @return a wrapped runnable in the current context, if there's no context the original runnable
   *     will be returned
   */
  public Runnable wrap(Runnable original) {
    if (DiscordContext.currentContext().isPresent()) {
      var context = DiscordContext.currentContext().get().copy();
      return () -> {
        var prev = DiscordContext.currentContext().orElse(null);
        DiscordContext.set(context);
        try {
          original.run();
        } finally {
          try {
            publisher.publishEvent(new DiscordContextTerminatedEvent(context));
          } catch (Throwable e) {
            logger.error("An error occurred while terminating event context", e);
          }
          DiscordContext.set(prev);
        }
      };
    } else {
      return original;
    }
  }

  /**
   * Wraps a consumer in the current DiscordContext
   *
   * @param original - original consumer
   * @return a wrapped consumer in the current context, if there's no context the original consumer
   *     will be returned
   */
  public <T> Consumer<T> wrap(Consumer<T> original) {
    if (DiscordContext.currentContext().isPresent()) {
      var context = DiscordContext.currentContext().get().copy();
      return (t) -> {
        var prev = DiscordContext.currentContext().orElse(null);
        DiscordContext.set(context);
        try {
          original.accept(t);
        } finally {
          try {
            publisher.publishEvent(new DiscordContextTerminatedEvent(context));
          } catch (Throwable e) {
            logger.error("An error occurred while terminating event context", e);
          }
          DiscordContext.set(prev);
        }
      };
    } else {
      return original;
    }
  }
}
