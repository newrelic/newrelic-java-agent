export NEW_RELIC_LICENSE_KEY=_NEW_RELIC_LICENSE_KEY_VALUE_
export _JVM_ARGS_ENV_VAR_NAME_=_JVM_ARGS_ENV_VAR_VALUE_

mkdir results
mkdir logs
DATETIME=$(date '+%Y-%m-%d-%H:%M:%S')

source ./config.sh

nohup sh ./start.sh > logs/app-"${DATETIME}".log &

if [ -z "${JMETER_DELAY}" ]; then
  "JMETER_DELAY is empty, setting the default value to 20"
  JMETER_DELAY=20
fi
echo "Waiting $JMETER_DELAY seconds until JMeter starts"
sleep $JMETER_DELAY


sh deps/jmeter/bin/jmeter.sh -n -t jmeter.jmx -l results/results.csv -j logs/jmeter-"${DATETIME}".log