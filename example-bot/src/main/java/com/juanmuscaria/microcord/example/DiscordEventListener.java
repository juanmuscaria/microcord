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
package com.juanmuscaria.microcord.example;

import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Example event listener for discord events.
// All JDA events are published through micronaut.event api
@Singleton
public class DiscordEventListener {
  private final Logger logger = LoggerFactory.getLogger(DiscordEventListener.class);

  // Listener for when a JDA instance is ready
  @EventListener
  public void onReady(ReadyEvent event) {
    logger.info("Bot shard ready, logged in as {}", event.getJDA().getSelfUser().getAsTag());
  }
}
