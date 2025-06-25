export NEW_RELIC_LICENSE_KEY=_NEW_RELIC_LICENSE_KEY_VALUE_
export _JVM_ARGS_ENV_VAR_NAME_=_JVM_ARGS_ENV_VAR_VALUE_

mkdir results
mkdir logs
DATETIME=$(date '+%Y-%m-%d-%H:%M:%S')

source ./config.sh

nohup sh ./start.sh > logs/app-"${DATETIME}".log &

sh deps/jmeter/bin/jmeter.sh -n -t jmeter.jmx -l results/results.csv -j logs/jmeter-"${DATETIME}".log