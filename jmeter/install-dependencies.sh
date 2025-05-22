rm -rf ./deps

JMETER_VERSION=5.6.3
JMXMON_VERSION=0.3

mkdir deps
cd deps

# Install the java agents

mkdir agents
cat ../config.json | jq .agents[] -c | while read -r agent
do
  agentPath=$(echo "${agent}" | jq .path -r)
  isPathUrl=$(echo "${agent}" | jq .isPathUrl -r)
  agentBuild=$(echo "${agent}" | jq .name -r)
  mkdir agents/"${agentBuild}"

  if [[ "$isPathUrl" == "true" ]];
  then
    curl --output agents/"${agentBuild}"/newrelic.jar "${agentPath}"
  else
    cp "${agentPath}" agents/"${agentBuild}"/newrelic.jar
  fi
done

# Install jmeter
echo "Installing JMeter"
curl --output jmeter.tgz https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz
tar -xzf jmeter.tgz
rm jmeter.tgz
mv apache-jmeter-${JMETER_VERSION} jmeter
echo "JMeter installed!"

# Install JMX Plugins

echo "Installing JMX sampler plugin for JMeter"
curl --output jpgc-jmxmon-${JMXMON_VERSION}.zip https://jmeter-plugins.org/files/packages/jpgc-jmxmon-${JMXMON_VERSION}.zip
unzip jpgc-jmxmon-${JMXMON_VERSION}.zip -d jpgc-jmxmon
mv jpgc-jmxmon/lib/*.jar jmeter/lib
mv jpgc-jmxmon/lib/ext/*.jar jmeter/lib/ext
rm jpgc-jmxmon-${JMXMON_VERSION}.zip
rm -rf jpgc-jmxmon
echo "Installed JMX sampler plugin for JMeter!"
