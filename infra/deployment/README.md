# Deployment

This folder contains all the required scripts to deploy the projects.

These scripts were tested with ansible 2.6.4, higher versions should work too, it might not work with smaller versions.

## Run
Execute the following commands to deploy the applications:
- bot-server: `ansible-playbook -i production-hosts.ini bot-server.yml`

## Troubleshooting
- Make sure the `production-hosts.ini` has the proper host (consider using `~/.ssh/config`).
- Make sure to set the proper Discord token on the `../server/src/main/resources/application.conf`.
