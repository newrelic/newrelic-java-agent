DATETIME=$(date '+%Y-%m-%d-%H:%M:%S')
echo "running Spring Pet Clinic with agent build ${AGENT_BUILD}"
nohup java -javaagent:./deps/agents/"${AGENT_BUILD}"/newrelic.jar \
           -Dmewrelic.config.file=./newrelic.yml \
           -Dnewrelic.logfile=logs/newrelic-"${DATETIME}".log \
           "${CUSTOM_JVM_ARGS}" "${COMMON_VM_ARGS}" \
           -jar build/libs/spring-petclinic-3.4.0.jar >logs/petclinic-"${DATETIME}".log &
sleep 20
sh deps/jmeter/bin/jmeter.sh -n -t test.jmx -l results/results.csv -j logs/jmeter-"${DATETIME}".log