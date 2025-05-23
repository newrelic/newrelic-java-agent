mkdir results
mkdir logs
export DATETIME=$(date '+%Y-%m-%d-%H:%M:%S')

source ./config.sh

nohup sh ./start.sh > logs/app-"${DATETIME}".log &

if [ -z "${JMETER_DELAY}" ]; then
  "JMETER_DELAY is empty, setting the default value to 20"
  JMETER_DELAY=20
fi
echo "Waiting $JMETER_DELAY seconds until JMeter starts"
sleep $JMETER_DELAY


sh deps/jmeter/bin/jmeter.sh -n -t jmeter.jmx -l results/results.csv -j logs/jmeter-"${DATETIME}".log