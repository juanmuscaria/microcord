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

import net.dv8tion.jda.api.interactions.commands.Command;

/**
 * Interface enums used for multiple choices must implement
 *
 * @param <T> A String, Long or Double, those are the only types supported in multiple choice
 *     options.
 */
public interface CommandChoices<T> {

  /**
   * Retrieves the value represented by this constant.
   *
   * @return the value this constant represents.
   */
  T value();

  /**
   * Internal method to build command choice data
   *
   * @return the choice data this constant represents.
   * @throws IllegalArgumentException if the subclass in not a compliant implementation.
   */
  default Command.Choice choiceData() {
    if (this instanceof Enum<?> enumValue) {
      var value = value();
      if (value instanceof String) {
        return new Command.Choice(enumValue.toString(), (String) value);
      } else if (value instanceof Long) {
        return new Command.Choice(enumValue.toString(), (long) value);
      } else if (value instanceof Double) {
        return new Command.Choice(enumValue.toString(), (double) value);
      } else {
        throw new IllegalArgumentException(
            "Invalid command choice implementation, the choice value must be a string, long or double.");
      }
    } else {
      throw new IllegalArgumentException(
          "Invalid command choice implementation, it must be an Enum.");
    }
  }

  /**
   * Internal method to retrieve a constant for given value.
   *
   * @param value the value representing a constant
   * @return the constant represented by the value
   * @throws IllegalArgumentException if there's no constant represented by the value or if the
   *     subclass in not a compliant implementation.
   */
  @SuppressWarnings("unchecked")
  default CommandChoices<T> fromValue(T value) {
    if (this instanceof Enum) {
      for (CommandChoices<?> constant : this.getClass().getEnumConstants()) {
        if (constant.value().equals(value)) {
          return (CommandChoices<T>) constant;
        }
      }
      throw new IllegalArgumentException("Value not found in the choice constants.");
    } else {
      throw new IllegalArgumentException(
          "Invalid command choice implementation, it must be an Enum.");
    }
  }
}
