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
package com.juanmuscaria.microcord;

import static com.juanmuscaria.microcord.JdaShardFactory.SHARD_MANAGER_KEY;

import com.juanmuscaria.microcord.configuration.JdaConfiguration;
import com.juanmuscaria.microcord.scope.DiscordContext;
import com.juanmuscaria.microcord.scope.DiscordContextData;
import com.juanmuscaria.microcord.scope.DiscordContextTerminatedEvent;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.*;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.naming.Described;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.runtime.context.scope.Refreshable;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import jakarta.inject.Singleton;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import net.dv8tion.jda.api.audio.factory.IAudioSendFactory;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.user.GenericUserEvent;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.hooks.InterfacedEventManager;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for holding an instance of {@link ShardManagerContainer} that use the default JDA shard
 * manager implementation.
 */
@SuppressWarnings("rawtypes")
@Factory
@Bean(typed = {DefaultShardFactory.class, JdaShardFactory.class})
@Requires(property = "discord.token")
public class DefaultShardFactory implements JdaShardFactory {

  private final ShardManagerContainer container;

  protected DefaultShardFactory(
      ApplicationContext applicationContext,
      ApplicationConfiguration applicationConfiguration,
      JdaConfiguration configuration,
      ApplicationEventPublisher publisher,
      @Any BeanProvider<JdaProviders> providers,
      @Any BeanProvider<IAudioSendFactory> audioSendFactory) {
    this.container =
        new DefaultShardContainer(
            applicationContext,
            applicationConfiguration,
            configuration,
            publisher,
            providers,
            audioSendFactory);
  }

  @NonNull @Primary
  @Singleton
  @Override
  public ShardManagerContainer getShardContainer() {
    return container;
  }

  @NonNull @Override
  @Refreshable({SHARD_MANAGER_KEY, JdaConfiguration.PREFIX})
  public ShardManager getShardManager() {
    return container.getShardManager();
  }
}

/**
 * Container for managing the lifecycle of {@link ShardManager} as an embedded application. Since
 * JDA ShardManager does not support a stop and start, the ShardManager is always rebuilt from
 * scratch, this also accommodates changed properties when the application is refreshed.
 */
class DefaultShardContainer implements ShardManagerContainer, Described {
  private static final Logger logger = LoggerFactory.getLogger(DefaultShardContainer.class);
  private final AtomicBoolean isRunning = new AtomicBoolean(true);
  private final ApplicationContext context;
  private final ApplicationConfiguration applicationConfiguration;
  private final JdaConfiguration configuration;

  @SuppressWarnings("rawtypes")
  private final ApplicationEventPublisher publisher;

  private final BeanProvider<JdaProviders> providers;
  private final BeanProvider<IAudioSendFactory> audioSendFactory;

  @NonNull private ShardManager shardManager;

  DefaultShardContainer(
      ApplicationContext context,
      ApplicationConfiguration applicationConfiguration,
      JdaConfiguration configuration,
      @SuppressWarnings("rawtypes") ApplicationEventPublisher publisher,
      BeanProvider<JdaProviders> providers,
      BeanProvider<IAudioSendFactory> audioSendFactory) {
    this.context = context;
    this.applicationConfiguration = applicationConfiguration;
    this.configuration = configuration;
    this.publisher = publisher;
    this.providers = providers;
    this.audioSendFactory = audioSendFactory;
    this.shardManager = build();
  }

  @NonNull @Override
  public ShardManager getShardManager() {
    return shardManager;
  }

  @Override
  public boolean isServer() {
    return true;
  }

  @Override
  public ApplicationContext getApplicationContext() {
    return context;
  }

  @Override
  public ApplicationConfiguration getApplicationConfiguration() {
    return applicationConfiguration;
  }

  @NonNull @Override
  public ShardManagerContainer start() {
    isRunning.set(true);
    shardManager.login();
    return this;
  }

  // ShardManager does not have a way to stop and start again with the same instance,
  // we need to create a whole new instance again
  @NonNull @Override
  public ShardManagerContainer stop() {
    logger.debug("Requested ShardManager stop, rebuilding managed instance.");

    shardManager.shutdown();
    shardManager = build();
    //noinspection unchecked
    publisher.publishEvent(
        new RefreshEvent(Collections.singletonMap(SHARD_MANAGER_KEY, "rebuild")));

    isRunning.set(false); //TODO: could a restart at the wrong time cause an application shutdown?
    return this;
  }

  @Override
  public boolean isRunning() {
    return isRunning.get();
  }

  private ShardManager build() {
    var intents = new HashSet<>(configuration.getIntents());
    configuration.getCacheFlags().stream()
        .map(CacheFlag::getRequiredIntent)
        .filter(Objects::nonNull)
        .forEach(intents::add);
    var builder =
        DefaultShardManagerBuilder.create(configuration.getToken(), intents)
            .disableCache(EnumSet.allOf(CacheFlag.class))
            .enableCache(configuration.getCacheFlags())
            .setEventManagerProvider(value -> new DefaultEventManager(publisher, value))
            .setShardsTotal(configuration.getShardsTotal());

    if (configuration.getShards().length > 0) {
      builder.setShards(configuration.getShards());
    }

    providers.ifPresent(
        p -> {
          p.getActivityProvider().ifPresent(builder::setActivityProvider);
          p.getIdleProvider().ifPresent(builder::setIdleProvider);
          p.getAudioPoolProvider().ifPresent(builder::setAudioPoolProvider);
          p.getStatusProvider().ifPresent(builder::setStatusProvider);
          p.getCallbackPoolProvider().ifPresent(builder::setCallbackPoolProvider);
          p.getGatewayPoolProvider().ifPresent(builder::setGatewayPoolProvider);
          p.getRateLimitPoolProvider().ifPresent(builder::setRateLimitPoolProvider);
        });

    audioSendFactory.ifPresent(builder::setAudioSendFactory);

    return builder.build(false);
  }

  @NonNull @Override
  public String getDescription() {
    return "Embedded JDA Runtime";
  }
}

class DefaultEventManager implements IEventManager {
  private static final Logger logger = LoggerFactory.getLogger(DefaultEventManager.class);
  private final IEventManager eventManager = new InterfacedEventManager();

  @SuppressWarnings("rawtypes")
  private final ApplicationEventPublisher publisher;

  private final int shardId;

  DefaultEventManager(
      @SuppressWarnings("rawtypes") ApplicationEventPublisher publisher, int shardId) {
    this.publisher = publisher;
    this.shardId = shardId;
  }

  @Override
  public void register(@NonNull Object listener) {
    eventManager.register(listener);
  }

  @Override
  public void unregister(@NonNull Object listener) {
    eventManager.unregister(listener);
  }

  @Override
  public void handle(@NonNull GenericEvent genericEvent) {
    User resolvedUser = null;
    DiscordLocale resolvedUserLocale = null;
    DiscordLocale resolvedGuildLocale = null;

    if (genericEvent instanceof GenericUserEvent event) {
      resolvedUser = event.getUser();
    }
    if (genericEvent instanceof GenericGuildEvent event) {
      resolvedGuildLocale = event.getGuild().getLocale();
    }
    if (genericEvent instanceof GenericGuildMemberEvent event) {
      resolvedUser = event.getUser();
    }
    if (genericEvent instanceof GenericInteractionCreateEvent event) {
      resolvedUser = event.getUser();
      resolvedUserLocale = event.getUserLocale();
      if (event.getGuild() != null) {
        resolvedGuildLocale = event.getGuildLocale();
      }
    }

    var ctx =
        new DiscordContextData(
            resolvedUser, resolvedUserLocale, resolvedGuildLocale, genericEvent.getJDA(), shardId);
    DiscordContext.set(ctx);
    try {
      //noinspection unchecked
      publisher.publishEvent(genericEvent);
      eventManager.handle(genericEvent);
    } finally {
      try {
        //noinspection unchecked
        publisher.publishEvent(new DiscordContextTerminatedEvent(ctx));
      } catch (Throwable e) {
        logger.error("An error occurred while terminating event context", e);
      }
      DiscordContext.set(null);
    }
  }

  @NonNull @Override
  public List<Object> getRegisteredListeners() {
    return eventManager.getRegisteredListeners();
  }
}
