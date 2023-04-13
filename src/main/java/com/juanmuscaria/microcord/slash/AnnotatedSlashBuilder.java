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

import com.juanmuscaria.microcord.annotations.SlashCommand;
import com.juanmuscaria.microcord.annotations.SlashCommand.OnCommand;
import com.juanmuscaria.microcord.annotations.SlashCommand.Option;
import com.juanmuscaria.microcord.utils.Triple;
import io.micronaut.context.BeanContext;
import io.micronaut.context.ExecutionHandleLocator;
import io.micronaut.context.MessageSource;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.MethodExecutionHandle;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Method processor for classes annotated with {@link SlashCommand}. It will automatically construct
 * a slash command tree from data extracted from the annotations.
 */
@Singleton
@AllArgsConstructor
public class AnnotatedSlashBuilder implements ExecutableMethodProcessor<SlashCommand> {
  private static final Pattern command = Pattern.compile("^[-_\\p{L}\\p{N}\\p{sc=Deva}\\p{sc=Thai}]{1,32}$");
  private static final String PLACEHOLDER =
      "__PLACEHOLDER__"; // Default placeholder for localized strings.
  private static final Logger logger = LoggerFactory.getLogger(AnnotatedSlashBuilder.class);

  // I wish this was not necessary, however I needed some sort of type conversion, maybe split this
  // to a ConversionService?
  // Primitive and boxed classes sure are a pain
  // Ordering is important keep implementations over parent interface or classes
  private static final List<Triple<Class<?>, OptionType, Function<OptionMapping, Object>>>
      TYPE_MAPPING =
          Arrays.asList(
              Triple.of(String.class, OptionType.STRING, OptionMapping::getAsString),
//              Triple.of(int.class, OptionType.INTEGER, OptionMapping::getAsInt), // TODO: not fail in an overflow?
//              Triple.of(Integer.class, OptionType.INTEGER, OptionMapping::getAsInt),
              Triple.of(long.class, OptionType.INTEGER, OptionMapping::getAsLong),
              Triple.of(Long.class, OptionType.INTEGER, OptionMapping::getAsLong),
              Triple.of(boolean.class, OptionType.BOOLEAN, OptionMapping::getAsBoolean),
              Triple.of(Boolean.class, OptionType.BOOLEAN, OptionMapping::getAsBoolean),
              Triple.of(User.class, OptionType.USER, OptionMapping::getAsUser),
              Triple.of(Member.class, OptionType.USER, OptionMapping::getAsMember),
              Triple.of(GuildChannelUnion.class, OptionType.CHANNEL, OptionMapping::getAsChannel),
              Triple.of(Role.class, OptionType.ROLE, OptionMapping::getAsRole),
              Triple.of(
                  IMentionable.class, OptionType.MENTIONABLE, OptionMapping::getAsMentionable),
              Triple.of(double.class, OptionType.NUMBER, OptionMapping::getAsDouble),
              Triple.of(Double.class, OptionType.NUMBER, OptionMapping::getAsDouble),
              Triple.of(Attachment.class, OptionType.ATTACHMENT, OptionMapping::getAsAttachment));

  @Getter
  private final Map<BeanDefinition<?>, CommandDefinition> commands =
      new ConcurrentHashMap<>(); // All registered commands goes here.

  // Injected micronaut stuff necessary for building the commands
  private final ExecutionHandleLocator executionHandleLocator;
  private final MessageSource messageSource;
  private final BeanContext ctx;

  // Process all "executable" methods within the target bean (class annotated with SlashCommand)
  @SuppressWarnings("unchecked")
  @Override
  public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
    // We are only interested in command methods
    method
        .getAnnotationTypeByStereotype(OnCommand.class)
        .ifPresent(
            exec -> {
              var commandName = orElseThrow(beanDefinition.stringValue(SlashCommand.class));
              var translationPrefix =
                  beanDefinition
                      .stringValue(SlashCommand.class, "translationKeyPrefix")
                      .orElse("interaction.slash.");

              // Initialize the command data if it's the first time encountering this bean
              var data = commands.get(beanDefinition);
              if (data == null) {
                data =
                    new CommandDefinition(
                        Commands.slash(commandName, PLACEHOLDER)
                            .setLocalizationFunction((key -> this.localize(translationPrefix, key)))
                            .setGuildOnly(
                                beanDefinition
                                    .booleanValue(SlashCommand.class, "guildOnly")
                                    .orElse(false))
                            .setNSFW(
                                beanDefinition
                                    .booleanValue(SlashCommand.class, "nsfw")
                                    .orElse(false)));

                // Work around the fact there's no "longValues"
                var guilds =
                    beanDefinition.getValue(SlashCommand.class, "guilds").orElse(new long[0]);
                if (guilds instanceof long[]) {
                  data.guilds = (long[]) guilds;
                }
                data.bean = ctx.getBean(beanDefinition);
                commands.put(beanDefinition, data);
              }

              var subCommand = method.stringValue(OnCommand.class).orElse("");
              if (subCommand.isEmpty()) {

                // Validate the current command state
                validate(
                    data.subCommandDefinitions != null,
                    "Cannot register a default executor in a command with sub-commands");
                validate(data.directExecution != null, "Command already has a default executor");

                // Register this as a single command
                data.slashCommand.addOptions(generateOptionsData(method));
                data.directExecution =
                    executionHandleLocator.createExecutionHandle(
                        beanDefinition, (ExecutableMethod<Object, ?>) method);
                logger.debug("Created single executor {} for {}", method, data);

              } else {

                // Validate the current command state
                validate(
                    data.directExecution != null,
                    "Cannot register a sub-command in a command with a default executor");
                if (data.subCommandDefinitions == null) {
                  data.subCommandDefinitions = new SubCommandDefinitions();
                }

                // Register this as a command with multiple sub-commands
                var subcommandData =
                    new SubcommandData(subCommand, PLACEHOLDER)
                        .addOptions(generateOptionsData(method));
                var commandGroup = method.stringValue(OnCommand.class, "group").orElse("");

                if (commandGroup.isEmpty()) {
                  // Single command without a command group, add it directly
                  data.slashCommand.addSubcommands(subcommandData);
                  data.subCommandDefinitions.subCommandExecution.put(
                      subCommand,
                      executionHandleLocator.createExecutionHandle(
                          beanDefinition, (ExecutableMethod<Object, ?>) method));
                } else {
                  // Initialize command group data
                  var commandGroupData = data.subCommandDefinitions.groupData.get(commandGroup);
                  if (commandGroupData == null) {
                    commandGroupData = new SubcommandGroupData(commandGroup, PLACEHOLDER);
                    data.subCommandDefinitions.groupData.put(commandGroup, commandGroupData);
                    data.slashCommand.addSubcommandGroups(commandGroupData);
                  }

                  commandGroupData.addSubcommands(subcommandData);
                  data.subCommandDefinitions.subCommandExecution.put(
                      commandGroup + subCommand,
                      executionHandleLocator.createExecutionHandle(
                          beanDefinition, (ExecutableMethod<Object, ?>) method));
                }
              }
            });
  }

  // Creates all option data for given executable method
  @SuppressWarnings("rawtypes")
  private List<OptionData> generateOptionsData(ExecutableMethod<?, ?> method) {
    var options = new ArrayList<OptionData>();
    for (Argument<?> argument : method.getArguments()) {
      // Only annotated arguments should be treated as a command option
      argument
          .findAnnotation(Option.class)
          .ifPresent(
              option -> {
                OptionData data;
                var optionName = orElseThrow(option.stringValue());
                var actualType = argument;
                if (argument.isOptional()) {
                  actualType = argument.getWrappedType();
                }

                if (Enum.class.isAssignableFrom(actualType.getType())
                    && CommandChoices.class.isAssignableFrom(actualType.getType())) {
                  // TODO: Maybe there's a better way to implement this whole choice system, but for
                  // now let's use enum + interface.
                  CommandChoices[] choices =
                      (CommandChoices[]) actualType.getType().getEnumConstants();
                  validate(choices.length == 0, "Command choices must have at least one element");

                  var choiceType = optionTypeOf(choices[0].value().getClass());
                  validate(
                      choiceType == OptionType.UNKNOWN || !choiceType.canSupportChoices(),
                      "Command choice value type is not supported");
                  data = new OptionData(choiceType, optionName, PLACEHOLDER);

                  for (CommandChoices choice : choices) {
                    data.addChoices(choice.choiceData());
                  }

                } else {
                  var choiceType = optionTypeOf(actualType.getType());
                  validate(choiceType == OptionType.UNKNOWN, "Invalid option type");
                  data = new OptionData(choiceType, optionName, PLACEHOLDER);
                }

                data.setRequired(!argument.isOptional());
                data.setAutoComplete(option.booleanValue("autocomplete").orElse(false));
                options.add(data);
              });
    }
    return options;
  }

  private Map<DiscordLocale, String> localize(String prefix, String localizationKey) {
    var map = new HashMap<DiscordLocale, String>();
    var key = prefix + localizationKey;
    for (DiscordLocale locale : DiscordLocale.values()) {
      if (locale == DiscordLocale.UNKNOWN) {
        continue;
      }
      ifPresentOrElse(
          messageSource.getMessage(key, Locale.forLanguageTag(locale.getLocale())),
          s -> map.put(locale, s),
          () -> {
            // TODO: Configurable default bot locale, don't assume it's english
            if (locale == DiscordLocale.ENGLISH_US) {
              logger.warn("Unlocalized command key found: {}", key);
              map.put(locale, trim(key.replace('.', '_').toLowerCase(Locale.ENGLISH)));
            }
          });
    }
    return map;
  }

  // TODO: Replace tis horrible thing
  private void validate(boolean expression, String message) {
    if (expression) {
      throw new IllegalArgumentException(message);
    }
  }

  // Trim a string to fit the 32 character limits discord requires
  private String trim(String s) {
    return s.substring(Math.max(0, s.length() - 32));
  }

  // TODO: Decouple this method
  OptionType optionTypeOf(Class<?> clazz) {
    for (Triple<Class<?>, OptionType, Function<OptionMapping, Object>> typeMap : TYPE_MAPPING) {
      if (typeMap.left().isAssignableFrom(clazz)) {
        return typeMap.middle();
      }
    }
    logger.warn("Invalid type {}", clazz);
    return OptionType.UNKNOWN;
  }

  // TODO: Decouple this method
  Object convertType(Class<?> clazz, OptionMapping optionMapping) {
    for (Triple<Class<?>, OptionType, Function<OptionMapping, Object>> typeMap : TYPE_MAPPING) {
      if (typeMap.left().isAssignableFrom(clazz)) {
        return typeMap.right().apply(optionMapping);
      }
    }
    throw new UnsupportedOperationException();
  }
}

// TODO: Decouple this class
@Getter
@ToString
class CommandDefinition {
  long[] guilds = new long[0];
  @Nonnull SlashCommandData slashCommand;
  @Nullable MethodExecutionHandle<?, Object> directExecution;
  @Nullable SubCommandDefinitions subCommandDefinitions;
  Object bean;

  CommandDefinition(@NonNull SlashCommandData slashCommand) {
    this.slashCommand = slashCommand;
  }
}

// TODO: Decouple this class
@Getter
@ToString
class SubCommandDefinitions {
  Map<String, MethodExecutionHandle<?, Object>> subCommandExecution = new HashMap<>();
  Map<String, SubcommandGroupData> groupData = new HashMap<>();
}
