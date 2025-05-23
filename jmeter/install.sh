CUSTOM_AGENT_PATH=""
CUSTOM_AGENT_NAME="custom"
IS_CUSTOM_AGENT_EXTERNAL=false

usage() {
 echo "Usage: $0 [OPTIONS]"
 echo "Options:"
 echo " -h, --help      Display this help message."
 echo " -n, --name      Custom agent build name. This determines the name of the directory the agent will be
                        installed in and identify it during performance tests. By default the value is \"custom\"."
 echo " -p, --path      Path to the custom jar. By default it is a file path but can be configured to be an http URL."
 echo " -e, --external  The provided path to the custom jar is treated as an http URL."
}

has_argument() {
    [[ ("$1" == *=* && -n ${1#*=}) || ( ! -z "$2" && "$2" != -*)  ]];
}

extract_argument() {
  echo "${2:-${1#*=}}"
}

handle_options() {
  while [ $# -gt 0 ]; do
    case $1 in
      -h | --help)
        usage
        exit 0
        ;;
      -n | --name*)
        if ! has_argument $@; then
          echo "Custom agent build name not specified." >&2
          usage
          exit 1
        fi
        CUSTOM_AGENT_NAME=$(extract_argument $@)
        ;;
      -p | --path*)
        if ! has_argument $@; then
          echo "Agent path not specified." >&2
          usage
          exit 1
        fi
        CUSTOM_AGENT_PATH=$(extract_argument $@)
        ;;
      -e | --external)
        IS_CUSTOM_AGENT_EXTERNAL=true
        ;;
      *) # Ignore
        ;;
    esac
    shift
  done
}

handle_options "$@"

rm -rf ./deps

JMETER_VERSION=5.6.3
JMXMON_VERSION=0.3

mkdir deps
cd deps || (echo "Failed to change directory to deps" && exit 1)

# Install the java agents
mkdir agents

function installAgent() {
      agentBuild="$1"
      agentPath="$2"
      isPathUrl="$3"
      mkdir agents/"${agentBuild}"
      if [[ "$isPathUrl" == "true" ]]; then
        if curl --output agents/"${agentBuild}"/newrelic.jar "${agentPath}" ; then
          echo "Successfully installed agent from url ${agentPath}"
        else
          echo "Failed to install agent from url ${agentPath}"
        fi
      else
        cp "${agentPath}" agents/"${agentBuild}"/newrelic.jar
        echo "Successfully installed agent from file ${agentPath}"
      fi
}

if [ -n "$CUSTOM_AGENT_PATH" ]; then
  installAgent "$CUSTOM_AGENT_NAME" "$CUSTOM_AGENT_PATH" "$IS_CUSTOM_AGENT_EXTERNAL"
fi

cat ../config.json | jq .agents[] -c | while read -r agent
do
  agentPath=$(echo "${agent}" | jq .path -r)
  agentBuild=$(echo "${agent}" | jq .name -r)
  isPathUrl=$(echo "${agent}" | jq .isPathUrl -r)
  installAgent "$agentBuild" "$agentPath" "$isPathUrl"
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
