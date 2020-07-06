# Deployment

This folder contains all the required scripts to deploy the projects.

These scripts were tested with ansible 2.9.10, higher versions should work but lower versions might not.

## Dependencies
- Make sure you can compile the [bot](/server)
- Prepare a linux server to deploy to, tested on ubuntu 16.04.
- Set the your server ip to your `~/.ssh/config`, for example:

```
Host scammer-detector-bot
    HostName 192.168.0.12
    User ubuntu
```

- Get your bot token from Discord, and set it on the [bot.env](config/bot.env) config file, consider copying the template for that [bot.env.template](config/bot.env.template).
- Set the proper config on the [application.conf](/server/src/main/resources/application.conf) to set the details for the servers that you will install the bot on.
- Make sure to give enough permissions to the bot on the channel you set on the previous config file, otherwise, you won't get any messages.

## Run
Execute the following commands to deploy the application:
- If your server has password-less sudo: `ansible-playbook -i production-hosts.ini bot-server.yml`
- Otherwise: `ansible-playbook -i production-hosts.ini --ask-become-pass bot-server.yml`


## Install bot
Once your bot is running, update and go to `https://discordapp.com/oauth2/authorize?client_id=[YOUR_BOT_CLIENT_ID]&scope=bot`

