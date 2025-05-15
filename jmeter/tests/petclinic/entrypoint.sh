DATETIME=$(date '+%Y-%m-%d-%H:%M:%S')
NEW_RELIC_APP_NAME="Jmeter Pet Clinic"
nohup java -Dcom.sun.management.jmxremote.port=1234 \
           -Dcom.sun.management.jmxremote.authenticate=false \
           -Dcom.sun.management.jmxremote.ssl=false \
           -javaagent:./deps/newrelic/newrelic.jar \
           -Dnewrelic.logfile=logs/newrelic-"${DATETIME}".log \
           -Dnewrelic.config.app_name="${NEW_RELIC_APP_NAME}" \
           "${JVM_ARGS}" \
           -jar build/libs/spring-petclinic-3.4.0.jar >logs/petclinic-"${DATETIME}".log &
sleep 20
sh deps/jmeter/bin/jmeter.sh -n -t test.jmx -l results/results.csv -j logs/load-test-jmeter-"${DATETIME}".log