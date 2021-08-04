#!/bin/bash
set -xv
lsb_release -a
uname -a
## Our private IP
PIP=$(hostname -I | awk '{print $1}')
echo "The private IP is ${PIP}"
## Test setting env variable from script:
echo "GHA_ENV_VAR=test_value" >> $GITHUB_ENV
## Test adding workspace bin to path:
# echo "${HOME}/ait-workspace/bin" >> $GITHUB_PATH
echo "Where is Java " $(whereis java)
echo "add Java to PATH"
echo "echo $(whereis java)" >> $GITHUB_PATH

## Check env variables - NOTE: Changes above are NOT relected here, NOT until a subsequent step or printenv command
printenv