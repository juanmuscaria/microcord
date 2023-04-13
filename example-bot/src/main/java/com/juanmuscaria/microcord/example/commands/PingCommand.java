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
package com.juanmuscaria.microcord.example.commands;

import com.juanmuscaria.microcord.annotations.SlashCommand;
import com.juanmuscaria.microcord.annotations.SlashCommand.OnCommand;
import com.juanmuscaria.microcord.annotations.SlashCommand.Option;
import com.juanmuscaria.microcord.scope.ScopedCallbacks;
import io.micronaut.context.LocalizedMessageSource;
import jakarta.inject.Inject;
import java.util.Optional;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.sharding.ShardManager;

// Example ping command
@SlashCommand("ping")
public class PingCommand {
  @Inject // Managed "ShardManager" provided by microcord
  ShardManager manager;
  @Inject // Allows to keep discord context (user language) across callbacks
  ScopedCallbacks scope;
  @Inject // Automatically localize messages based on the user language
  private LocalizedMessageSource messageSource;

  // @OnCommand annotations defines the method that will respond to user commands
  // @Option annotations allows to define command options from java types, "Optional" types
  // automatically makes
  //  the option not required
  @OnCommand
  public void ping(SlashCommandInteractionEvent event, @Option("shard") Optional<Long> shardId) {
    shardId.ifPresentOrElse(
        id -> {
          var shard = manager.getShardById(id.intValue());
          if (shard != null) {
            shard
                .getRestPing()
                .queue(
                    scope.wrap(
                        (ping) ->
                            event
                                .reply(localize("interaction.slash.ping.response", ping))
                                .queue()));
          } else {
            event.reply(localize("interaction.slash.ping.invalid_shard")).queue();
          }
        },
        () ->
            event
                .getJDA()
                .getRestPing()
                .queue(
                    scope.wrap(
                        (ping) ->
                            event
                                .reply(localize("interaction.slash.ping.response", ping))
                                .queue())));
  }

  // Helper method to localize message keys or return the key as text if not present making it
  // easier to debug missing translation keys
  private String localize(String text, Object... variables) {
    return messageSource.getMessageOrDefault(text, text, variables);
  }
}
