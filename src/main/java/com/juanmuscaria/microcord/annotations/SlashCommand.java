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
package com.juanmuscaria.microcord.annotations;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.DefaultScope;
import io.micronaut.context.annotation.Executable;
import jakarta.inject.Singleton;
import java.lang.annotation.*;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction;

/** Indicates a class is a slash command handler */
@SuppressWarnings("unused") // None of this is really unused
@Bean
@DefaultScope(Singleton.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface SlashCommand {

  /**
   * Slash command name, command names must match the following regex: {@code
   * ^[-_\p{L}\p{N}\p{sc=Deva}\p{sc=Thai}]{1,32}$} with the unicode flag set. If there is a
   * lowercase variant of any letters used, you must use those. Characters with no lowercase
   * variants and/or uncased letters are still allowed.
   *
   * @return the Application Slash Command name
   */
  String value();

  /**
   * Indicates this is an age restricted command.
   *
   * @return whether the command is age-restricted.
   */
  boolean nsfw() default false;

  /**
   * Indicates this is a command only usable in a guild. Only affects global commands.
   *
   * @return whether this command is only usable in a guild.
   */
  boolean guildOnly() default false;

  /**
   * List of guild IDs this command will be available to. This implicitly makes this command a guild
   * command instead of global command.
   *
   * @return the guild IDs this command will be available to.
   */
  long[] guilds() default {};

  /**
   * Translation key prefix applied to localization lookups, used to group up command localizations.
   *
   * @return the translation key prefix for this command.
   * @see LocalizationFunction
   */
  String translationKeyPrefix() default "interaction.slash.";

  /**
   * Indicates a method is a slash command executor. It can also define the executor for a
   * subcommand.
   *
   * <p>Command executors must be a public void member method.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  @Executable
  @Inherited
  @interface OnCommand {
    /**
     * Slash subcommand name, must follow the same restrictions as a command name.
     *
     * <p>If left empty it will act as the default slash command executor.
     *
     * @return the name of this subcommand.
     */
    String value() default "";

    /**
     * Group this command belongs to, must follow the same restrictions as a command name.
     *
     * @return the group this command belongs to.
     */
    String group() default "";
  }

  /**
   * Defines a method parameter as a command option. Supported types are:
   * <ul>
   *     <li>{@link String} - {@link OptionType#STRING}</li>
   *     <li>{@link Long}/long - {@link OptionType#INTEGER}</li>
   *     <li>{@link Boolean}/boolean - {@link OptionType#BOOLEAN}</li>
   *     <li>{@link User} - {@link OptionType#USER}</li>
   *     <li>{@link Member} - {@link OptionType#USER}</li>
   *     <li>{@link GuildChannelUnion} - {@link OptionType#CHANNEL}</li>
   *     <li>{@link Role} - {@link OptionType#ROLE}</li>
   *     <li>{@link IMentionable} - {@link OptionType#MENTIONABLE}</li>
   *     <li>{@link Double}/double - {@link OptionType#NUMBER}</li>
   *     <li>{@link Attachment} - {@link OptionType#ATTACHMENT}</li>
   * </ul>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER})
  @interface Option {
    /**
     * Option name, must follow the same restrictions as a command name.
     *
     * @return the name of this option.
     */
    String value();

    boolean autocomplete() default false;
  }
}
