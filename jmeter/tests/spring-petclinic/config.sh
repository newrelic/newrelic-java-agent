# Use the same docker image name in docker-compose.yml
export DOCKER_IMAGE_NAME="jmeter/petclinic:latest"
# Time delay from when we start the Java app till we start Jmeter. This is for apps that take time to startup
export JMETER_DELAY=20
# Name of the environment variable used to store the JVM arguments.
# You want to use that environment in start.sh if needed.
export JVM_ARGS_ENV_VAR_NAME="JAVA_OPTS"