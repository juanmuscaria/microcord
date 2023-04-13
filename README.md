# µcord - a Micronaut library for discord bots.

Microcord is a micronaut library powered by [JDA](https://github.com/DV8FromTheWorld/JDA) for creating discord bots as
micronaut applications taking leverage of dependency injection and the power of AOP. This library will manage JDA for 
you and allow you to focus in writing the actual business logic of your bot with easy to use API designed similar to web
controllers.

# Getting started 

## Adding to your project
This library is an embedded application/server similar to the netty runtime, make sure to remove `runtime("netty")` from 
your micronaut block in your `build.gradle`.

## Configuring JDA
Before you can begin making a bot you need to provide some basic configuration to be able to connect your bot.
```yaml
discord:
  # Your bot token, ${} is used to refer to an environment variable.
  token: '${DISCORD_TOKEN}'
  
  # Default JDA cache flags list, uncomment to customize
  # cache-flags: [ACTIVITY, VOICE_STATE, EMOJI, STICKER, CLIENT_STATUS, MEMBER_OVERRIDES, ROLE_TAGS, FORUM_TAGS, ONLINE_STATUS, SCHEDULED_EVENTS]
  
  # Default JDA intents list, uncomment to customize, cache intents are implicitly added.
  # intents: [GUILD_BANS, GUILD_MODERATION, GUILD_EMOJIS_AND_STICKERS, GUILD_INVITES, GUILD_VOICE_STATES, GUILD_MESSAGES, GUILD_MESSAGE_REACTIONS, DIRECT_MESSAGES, DIRECT_MESSAGE_REACTIONS, SCHEDULED_EVENTS]
  
  # Default JDA shard total, -1 for Discord recommended shard total,
  # shards-total: -1
  
  # Default JDA shards list, uncomment for manual configuration of shards. Can be used for external management of bot instances and clusters.
  # shards: []
```

## Creating a bot
Checkout the `example-bot` subproject for a "how to" in using this library
