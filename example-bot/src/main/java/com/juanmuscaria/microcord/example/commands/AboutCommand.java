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
import io.micronaut.context.LocalizedMessageSource;
import jakarta.inject.Inject;
import java.awt.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@SlashCommand("about")
public class AboutCommand {
  @Inject // Automatically localize messages based on the user language
  private LocalizedMessageSource messageSource;

  @OnCommand
  public void about(SlashCommandInteractionEvent event) {
    var embed = new EmbedBuilder();
    embed
        .setAuthor(
            event.getJDA().getSelfUser().getAsTag(), event.getJDA().getSelfUser().getAvatarUrl())
        .setTitle(localize("interaction.slash.about.title", event.getJDA().getSelfUser().getName()))
        .setDescription(localize("interaction.slash.about.body"))
        .setColor(Color.MAGENTA)
        .setFooter(localize("interaction.slash.about.footer"));
    event.replyEmbeds(embed.build()).queue();
  }

  // Helper method to localize message keys or return the key as text if not present making it
  // easier to debug missing translation keys
  private String localize(String text, Object... variables) {
    return messageSource.getMessageOrDefault(text, text, variables);
  }
}
