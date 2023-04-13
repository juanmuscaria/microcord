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
package com.juanmuscaria;

import com.juanmuscaria.microcord.JdaShardFactory;
import com.juanmuscaria.microcord.configuration.JdaConfiguration;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.PropertySource;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.EnumSet;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@MicronautTest
@EnabledIfEnvironmentVariable(named = "DISCORD_TOKEN", matches = ".*")
class DefaultShardFactoryTest {

  @Inject private ApplicationContext context;
  @Inject private JdaShardFactory jdaShardFactory;
  @Inject ShardManager shardManager;
  @Inject JdaConfiguration configuration;

  @Test
  void testShardManagerContainerSingleton() {
    Assertions.assertEquals(
        jdaShardFactory.getShardContainer(), jdaShardFactory.getShardContainer());
    Assertions.assertEquals(jdaShardFactory.getShardManager(), jdaShardFactory.getShardManager());
  }

  @SuppressWarnings("resource")
  @Test
  void testRefreshShardManagerContainer() {
    var container = jdaShardFactory.getShardContainer();
    var prevShard = container.getShardManager();
    var prevConfig = configuration.getCacheFlags();

    // Note, due to injected fields being a proxy checks needs to be done against some known object
    // within shard manager.
    Assertions.assertEquals(prevShard.getShardCache(), shardManager.getShardCache());

    context
        .getEnvironment()
        .addPropertySource(
            PropertySource.of(
                Collections.singletonMap("discord.cache-flags", EnumSet.noneOf(CacheFlag.class))));

    container.refresh();

    Assertions.assertNotEquals(prevConfig, configuration.getCacheFlags());
    Assertions.assertNotEquals(
        prevShard.getShardCache(), container.getShardManager().getShardCache());
    Assertions.assertNotEquals(prevShard.getShardCache(), shardManager.getShardCache());
    Assertions.assertEquals(
        container.getShardManager().getShardCache(), shardManager.getShardCache());
  }
}
