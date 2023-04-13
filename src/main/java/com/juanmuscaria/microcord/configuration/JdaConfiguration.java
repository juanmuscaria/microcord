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
package com.juanmuscaria.microcord.configuration;

import static net.dv8tion.jda.api.requests.GatewayIntent.*;

import io.micronaut.context.annotation.ConfigurationProperties;
import java.util.EnumSet;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

@ConfigurationProperties(JdaConfiguration.PREFIX)
@Getter
@Setter
public class JdaConfiguration {
  public static final String PREFIX = "discord";
  @NotBlank private String token;
  private Set<CacheFlag> cacheFlags = EnumSet.allOf(CacheFlag.class);
  private Set<GatewayIntent> intents = getDefaultIntents();
  private int shardsTotal = -1;
  private int[] shards = new int[0];

  private static Set<GatewayIntent> getDefaultIntents() {
    var intents = EnumSet.allOf(GatewayIntent.class);
    intents.removeAll(
        EnumSet.of(
            GUILD_MEMBERS,
            GUILD_PRESENCES,
            MESSAGE_CONTENT,
            GUILD_WEBHOOKS,
            GUILD_MESSAGE_TYPING,
            DIRECT_MESSAGE_TYPING));
    return intents;
  }
}
