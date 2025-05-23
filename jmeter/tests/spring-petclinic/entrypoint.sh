DATETIME=$(date '+%Y-%m-%d-%H:%M:%S')

echo "running Spring Pet Clinic with agent path: ${AGENT_JAR_PATH}"
echo "running Spring Pet Clinic with agent config path: ${NEW_RELIC_CONFIG_PATH}"

nohup java -javaagent:"${AGENT_JAR_PATH}" \
           -Dnewrelic.config.file="${NEW_RELIC_CONFIG_PATH}" \
           -Dnewrelic.logfile=logs/newrelic-"${DATETIME}".log \
           ${CUSTOM_JVM_ARGS} \
           ${JMX_JVM_ARGS} \
           -jar build/libs/spring-petclinic-3.4.0.jar >logs/petclinic-"${DATETIME}".log &
sleep 20
sh deps/jmeter/bin/jmeter.sh -n -t test.jmx -l results/results.csv -j logs/jmeter-"${DATETIME}".log