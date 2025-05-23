if [ -f ".env" ]
then
    echo "Getting environment variables from .env file"
    source .env
else
    echo ".env not found, will use the environment variables NEW_RELIC_LICENSE_KEY and CUSTOM_JVM_ARGS from the existing environment"
fi

export DATETIME=$(date '+%Y-%m-%d-%H:%M:%S')

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

echo "Running tests for each build of the Java agent"
for agentPath in ../../deps/agents/*; do
  AGENT_BUILD=$(basename "${agentPath}")

  AGENT_JAR_PATH="./deps/agents/${AGENT_BUILD}/newrelic.jar"

  echo "Found agent build ${AGENT_BUILD}"

  RESULTS_AGENT_BUILD_DIR=./results/"${AGENT_BUILD}"
  mkdir "${RESULTS_AGENT_BUILD_DIR}"
  echo "Made directory ${RESULTS_AGENT_BUILD_DIR}"

  LOGS_AGENT_BUILD_DIR=./logs/"${AGENT_BUILD}"
  mkdir "${LOGS_AGENT_BUILD_DIR}"
  echo "Made directory ${LOGS_AGENT_BUILD_DIR}"

  for testCase in test_cases/*; do
    TEST_CASE=$(basename "${testCase}")
    echo "Running test using agent build ${AGENT_BUILD} and test case ${TEST_CASE}"

    NEW_RELIC_CONFIG_PATH=./test_cases/"${TEST_CASE}"/newrelic.yml

    export JVM_ARGS="-javaagent:${AGENT_JAR_PATH}
      -Dnewrelic.config.file=${NEW_RELIC_CONFIG_PATH}
      -Dnewrelic.logfile=logs/newrelic-${DATETIME}.log
      ${CUSTOM_JVM_ARGS}
      ${JMX_JVM_ARGS}"
    printf "Set JVM args to be:\n%s" "${JVM_ARGS}"

    echo "Starting up docker compose"
    docker compose up
    echo "Removing docker compose images"
    docker compose rm -f

    TEST_CASE_DIRECTORY="${RESULTS_AGENT_BUILD_DIR}"/"${TEST_CASE}"
    RESULTS_CURRENT_RUN_DIR=./results/currentRun

    echo "Renaming ${RESULTS_CURRENT_RUN_DIR} to ${TEST_CASE_DIRECTORY}"
    mv ${RESULTS_CURRENT_RUN_DIR} "${TEST_CASE_DIRECTORY}"

    LOGS_DIRECTORY="${LOGS_AGENT_BUILD_DIR}"/"${TEST_CASE}"
    LOGS_CURRENT_RUN_DIR=./logs/currentRun

    echo "Renaming ${LOGS_CURRENT_RUN_DIR} to ${LOGS_DIRECTORY}"
    mv ${LOGS_CURRENT_RUN_DIR} "${LOGS_DIRECTORY}"

    echo "Removing directories ${RESULTS_CURRENT_RUN_DIR} and ${LOGS_CURRENT_RUN_DIR}"
    rm -rf ${RESULTS_CURRENT_RUN_DIR}
    rm -rf ${LOGS_CURRENT_RUN_DIR}
  done
done

echo "Removing docker image ${DOCKER_IMAGE_NAME}"
docker image rm "${DOCKER_IMAGE_NAME}"