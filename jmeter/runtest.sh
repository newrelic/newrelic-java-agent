if [ -f "./.env" ]
then
    echo "Getting environment variables from .env file"
    source .env
else
    echo ".env not found, will use the environment variables NEW_RELIC_LICENSE_KEY and CUSTOM_JVM_ARGS from the existing environment"
fi

export DATETIME
DATETIME=$(date '+%Y-%m-%d-%H:%M:%S')

export NEW_RELIC_LICENSE_KEY

TEST_DIR=$1
echo "Loading environment variables from ${TEST_DIR}/config.sh"
source "${TEST_DIR}"/config.sh

echo "Building docker image ${DOCKER_IMAGE_NAME}"

docker build --no-cache . -f "${TEST_DIR}"/DOCKERFILE -t "${DOCKER_IMAGE_NAME}"

cd "${TEST_DIR}" || exit 1
echo "Changed directory to $(pwd)"

JMX_JVM_ARGS="-Dcom.sun.management.jmxremote.port=1234
  -Dcom.sun.management.jmxremote.authenticate=false
  -Dcom.sun.management.jmxremote.ssl=false"

echo "Making logs and results directories"
mkdir ./logs
mkdir ./results

function runTest() {
  AGENT_BUILD=$1
  AGENT_JVM_ARG=$2
  RESULTS_AGENT_BUILD_DIR=$3
  LOGS_AGENT_BUILD_DIR=$4
  TEST_CASE=$5

  if [[ "$AGENT_BUILD" == "without-agent" ]]; then
    echo "Running test without the agent"
    RESULTS_DIRECTORY="${RESULTS_AGENT_BUILD_DIR}"
    LOGS_DIRECTORY="${LOGS_AGENT_BUILD_DIR}"
    NEW_RELIC_CONFIG_JVM_ARG=""
  else
    echo "Running test using agent build ${AGENT_BUILD} and test case ${TEST_CASE}"
    NEW_RELIC_CONFIG_JVM_ARG="-Dnewrelic.config.file=./test_cases/${TEST_CASE}/newrelic.yml"
    RESULTS_DIRECTORY="${RESULTS_AGENT_BUILD_DIR}"/"${TEST_CASE}"
    LOGS_DIRECTORY="${LOGS_AGENT_BUILD_DIR}"/"${TEST_CASE}"
  fi

  export JVM_ARGS="${AGENT_JVM_ARG}
    ${NEW_RELIC_CONFIG_JVM_ARG}
    -Dnewrelic.logfile=logs/newrelic-${DATETIME}.log
    ${CUSTOM_JVM_ARGS}
    ${JMX_JVM_ARGS}"
  printf "Set JVM args to be:\n%s" "${JVM_ARGS}"

  echo "Starting up docker compose"
  docker compose up
  echo "Removing docker compose images"
  docker compose rm -f

  RESULTS_CURRENT_RUN_DIR=./results/currentRun
  echo "Renaming ${RESULTS_CURRENT_RUN_DIR} to ${RESULTS_DIRECTORY}"
  mv ${RESULTS_CURRENT_RUN_DIR} "${RESULTS_DIRECTORY}"

  LOGS_CURRENT_RUN_DIR=./logs/currentRun
  echo "Renaming ${LOGS_CURRENT_RUN_DIR} to ${LOGS_DIRECTORY}"
  mv ${LOGS_CURRENT_RUN_DIR} "${LOGS_DIRECTORY}"

  echo "Removing directories ${RESULTS_CURRENT_RUN_DIR} and ${LOGS_CURRENT_RUN_DIR}"
  rm -rf ${RESULTS_CURRENT_RUN_DIR}
  rm -rf ${LOGS_CURRENT_RUN_DIR}
}

function runPerformanceTest() {
    AGENT_BUILD=$1
    if [[ -n "$AGENT_BUILD" ]]; then
      AGENT_JVM_ARG="-javaagent:./deps/agents/${AGENT_BUILD}/newrelic.jar"
      echo "Found agent build ${AGENT_BUILD}"
    else
      AGENT_JVM_ARG=""
      AGENT_BUILD="without-agent"
      echo "Running tests with no agent"
    fi

    RESULTS_AGENT_BUILD_DIR=./results/"${AGENT_BUILD}"
    mkdir "${RESULTS_AGENT_BUILD_DIR}"
    echo "Made directory ${RESULTS_AGENT_BUILD_DIR}"

    LOGS_AGENT_BUILD_DIR=./logs/"${AGENT_BUILD}"
    mkdir "${LOGS_AGENT_BUILD_DIR}"
    echo "Made directory ${LOGS_AGENT_BUILD_DIR}"

    if [[ "$AGENT_BUILD" == "without-agent" ]]; then
      runTest ${AGENT_BUILD} "${AGENT_JVM_ARG}" ${RESULTS_AGENT_BUILD_DIR} ${LOGS_AGENT_BUILD_DIR} "without-agent"
    else
      for testCase in test_cases/*; do
        runTest ${AGENT_BUILD} "${AGENT_JVM_ARG}" ${RESULTS_AGENT_BUILD_DIR} ${LOGS_AGENT_BUILD_DIR} "$(basename "${testCase}")"
      done
    fi
}

echo "Running test for control case (no agent)"
runPerformanceTest

echo "Running tests for each build of the Java agent"
for agentPath in ../../deps/agents/*; do
  runPerformanceTest "$(basename "${agentPath}")"
done

echo "Removing docker image ${DOCKER_IMAGE_NAME}"
docker image rm "${DOCKER_IMAGE_NAME}"