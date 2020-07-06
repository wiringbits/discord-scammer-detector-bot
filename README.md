# Discord Scammer Detector Bot
This is a simple bot that once installed on a discord server, it will analyze new users to detect potential scammers.

A potential scammer is anyone with a username/nickname very similar to the server team members.

For that, it takes the [Levenshtein distance](https://en.wikipedia.org/wiki/Levenshtein_distance) from the new members and the trusted members, if the value is small enough, the bot notifies about it to the configured channel.

Try it by running the [server](server) locally, or deploying by following the [instructions](infra/deployment).

## TODOs
- Once this is tested enough, the plan is to ban potential scammers automatically.
- Detect team members from the discord servers instead of requiring the members to be placed in the config file.
- Analyze existing members when the bot gets installed.
