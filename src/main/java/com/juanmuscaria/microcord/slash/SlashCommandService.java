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
package com.juanmuscaria.microcord.slash;

import static com.juanmuscaria.microcord.utils.OptionalConveniences.ifPresentOrElse;
import static com.juanmuscaria.microcord.utils.OptionalConveniences.orElseThrow;

import com.juanmuscaria.microcord.annotations.SlashCommand.Option;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.MethodExecutionHandle;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to handle incoming {@link SlashCommandInteractionEvent} and appropriately call the method
 * executors linked to it
 */
@Singleton
@AllArgsConstructor
public class SlashCommandService {
  private static final boolean REMOVE_INVALID_CMD = false;
  private static final Logger logger = LoggerFactory.getLogger(SlashCommandService.class);
  private final ConcurrentMap<Long, Pair<Command, CommandDefinition>> commands =
      new ConcurrentHashMap<>();
  private final AnnotatedSlashBuilder annotatedSlashBuilder;

  @EventListener
  public void registerCommands(ReadyEvent event) {
    var jda = event.getJDA();
    for (CommandDefinition commandDefinition : annotatedSlashBuilder.getCommands().values()) {
      if (commandDefinition.getGuilds().length > 0) {
        for (long guildId : commandDefinition.getGuilds()) {
          var guild = jda.getGuildById(guildId);
          if (guild != null) {
            guild
                .upsertCommand(commandDefinition.getSlashCommand())
                .onSuccess(
                    (command) -> {
                      logger.debug("Registered command {}", command.getName());
                      commands.put(command.getIdLong(), Pair.of(command, commandDefinition));
                    })
                .queue();
          }
        }
      } else {
        jda.upsertCommand(commandDefinition.getSlashCommand())
            .onSuccess(
                (command) -> {
                  logger.debug("Registered command {}", command.getName());
                  commands.put(command.getIdLong(), Pair.of(command, commandDefinition));
                })
            .queue();
      }
    }
  }

  @EventListener
  public void processSlashEvent(SlashCommandInteractionEvent event) {
    if (commands.containsKey(event.getCommandIdLong())) {
      var command = commands.get(event.getCommandIdLong()).getRight();
      if (command.getDirectExecution() != null) {
        invokeCommand(event, command.getDirectExecution());
      } else {
        var key =
            event.getSubcommandGroup() != null
                ? event.getSubcommandGroup() + event.getSubcommandName()
                : event.getSubcommandName();
        assert command.subCommandDefinitions != null;
        invokeCommand(event, command.subCommandDefinitions.subCommandExecution.get(key));
      }
    } else {
      logger.warn("Invalid command with ID:{}", event.getCommandIdLong());
      if (REMOVE_INVALID_CMD) {
        event.getJDA().deleteCommandById(event.getCommandIdLong()).queue();
      }
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void invokeCommand(
      SlashCommandInteractionEvent event, MethodExecutionHandle<?, Object> method) {
    // DiscordUserContext.set(new DiscordUser(event.getUser(), event.getUserLocale()));
    var arguments = method.getArguments();
    if (arguments.length == 0) {
      method.invoke();
    } else {
      var argumentList = new ArrayList<>(arguments.length);
      for (Argument<?> argument : arguments) {
        ifPresentOrElse(
            argument.findAnnotation(Option.class),
            (option -> {
              var optionName = orElseThrow(option.stringValue());
              var actualType = argument;
              if (argument.isOptional()) {
                actualType = argument.getWrappedType();
              }
              if (argument.isOptional() && event.getOption(optionName) == null) {
                argumentList.add(Optional.empty());
              } else if (Enum.class.isAssignableFrom(actualType.getType())
                  && CommandChoices.class.isAssignableFrom(actualType.getType())) {
                CommandChoices[] choices =
                    (CommandChoices[]) actualType.getType().getEnumConstants();
                var value =
                    choices[0].fromValue(
                        annotatedSlashBuilder.convertType(
                            choices[0].value().getClass(), event.getOption(optionName)));
                argumentList.add(argument.isOptional() ? Optional.of(value) : value);
              } else {
                var value =
                    annotatedSlashBuilder.convertType(
                        actualType.getType(), event.getOption(optionName));
                argumentList.add(argument.isOptional() ? Optional.of(value) : value);
              }
            }),
            () -> {
              if (SlashCommandInteractionEvent.class.isAssignableFrom(argument.getType())) {
                argumentList.add(event);
              } else {
                throw new IllegalArgumentException("Unknown argument type");
              }
            });
      }

      method.invoke(argumentList.toArray());
    }
  }
}
