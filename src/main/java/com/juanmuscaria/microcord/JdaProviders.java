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

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.IntFunction;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ThreadPoolProvider;

/** Intended to customize the providers delivered to the shard manager builder */
public interface JdaProviders {

  /**
   * @see DefaultShardManagerBuilder#setStatusProvider(IntFunction)
   */
  default Optional<IntFunction<? extends Activity>> getActivityProvider() {
    return Optional.empty();
  }

  /**
   * @see DefaultShardManagerBuilder#setIdleProvider(IntFunction)
   */
  default Optional<IntFunction<Boolean>> getIdleProvider() {
    return Optional.empty();
  }

  /**
   * @see DefaultShardManagerBuilder#setAudioPoolProvider(ThreadPoolProvider)
   */
  default Optional<ThreadPoolProvider<? extends ScheduledExecutorService>> getAudioPoolProvider() {
    return Optional.empty();
  }

  /**
   * @see DefaultShardManagerBuilder#setStatusProvider(IntFunction)
   */
  default Optional<IntFunction<OnlineStatus>> getStatusProvider() {
    return Optional.empty();
  }

  /**
   * @see DefaultShardManagerBuilder#setCallbackPoolProvider(ThreadPoolProvider)
   */
  default Optional<ThreadPoolProvider<? extends ExecutorService>> getCallbackPoolProvider() {
    return Optional.empty();
  }

  /**
   * @see DefaultShardManagerBuilder#setGatewayPoolProvider(ThreadPoolProvider)
   */
  default Optional<ThreadPoolProvider<? extends ScheduledExecutorService>>
      getGatewayPoolProvider() {
    return Optional.empty();
  }

  /**
   * @see DefaultShardManagerBuilder#setRateLimitPool(ScheduledExecutorService)
   */
  default Optional<ThreadPoolProvider<? extends ScheduledExecutorService>>
      getRateLimitPoolProvider() {
    return Optional.empty();
  }
}
