if [ -f ".env" ]
then
    source .env
else
    echo ".env not found, please copy .env.template and fill the respective values"
fi

echo "Building docker image jmeter/${TEST_DIR}"

TEST_DIR=$1
docker build --no-cache . -f ./tests/"${TEST_DIR}"/DOCKERFILE -t jmeter/"${TEST_DIR}"

cd tests/"${TEST_DIR}" || exit
echo "Changed directory to $(pwd)"

export JMX_JVM_ARGS=" -Dcom.sun.management.jmxremote.port=1234 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false "

for agentPath in ../../deps/agents/*; do
  export AGENT_BUILD=$(basename "${agentPath}")
  echo "running tests for agent build ${AGENT_BUILD}"
  docker compose up
  docker compose rm -f
  mv ./results/currentRun ./results/"${AGENT_BUILD}"
done
docker image rm jmeter/"${TEST_DIR}":latest