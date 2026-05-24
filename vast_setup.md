# Gateway Configuration
On vast.ai edit the template, in docker options add -p 8001:8001
Launch instance with template ; when launched click on terminal button to get ssh command
In local apply this command to forward ssh key, needed to checkout code on github:

eval "$(ssh-agent -s)" ; ssh-add ~/.ssh/id_rsa

Then ssh to the gateway (add -A option to forward ssh key):
ssh -A ...

# Installation on Gateway
curl -fsSL https://raw.githubusercontent.com/davidjdiebold/forge/gamerunnerapi/install.sh | bash
cd forge/forge ; source run.sh

# Get Service URL
On vast.ai UI, on instance, there is a widget that gives external urls for each port.

