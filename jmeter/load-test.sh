DATETIME=$(date '+%Y-%m-%d-%H:%M:%S')
sh deps/jmeter/bin/jmeter.sh -n -t load-test.jmx -l results/load-test-results.csv -j logs/load-test-jmeter-${DATETIME}.log