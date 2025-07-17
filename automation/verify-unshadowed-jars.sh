#!/bin/bash

TMP_DIR=$(mktemp -d)
cp ./newrelic-agent/build/newrelicJar/newrelic.jar "$TMP_DIR"
cd "$TMP_DIR"
unzip -q ./newrelic.jar

# Create a temp file of the unshadowed jars we expect in the agent jar
EXPECTED_FILES_LIST="./expected-files.txt"
cat << EOF > "$EXPECTED_FILES_LIST"
agent-bridge-datastore.jar
agent-bridge.jar
newrelic-api.jar
newrelic-security-agent.jar
newrelic-security-api.jar
newrelic-weaver-api.jar
newrelic-weaver-scala-api.jar
newrelic.jar
EOF

# Find the jar files in the root folder and dump them to a file to be compared
# against the expected file list
ACTUAL_FILES_LIST="./actual-files.txt"
find . -maxdepth 1 -type f -name "*.jar" \
    -not -name "$(basename "$0")" \
    -not -name "$(basename "$EXPECTED_FILES_LIST")" \
    -not -name "$(basename "$ACTUAL_FILES_LIST")" \
    -print0 | xargs -0 -n 1 basename | sort > "$ACTUAL_FILES_LIST"

diff -q "$EXPECTED_FILES_LIST" "$ACTUAL_FILES_LIST"

if [[ $? -ne 0 ]]; then
    echo ":x: Failure: Extra jars detected in the root folder of the agent jar. Check output for list of jars." >> "$GITHUB_STEP_SUMMARY"
    echo "Unexpected jars:"
    diff "$EXPECTED_FILES_LIST" "$ACTUAL_FILES_LIST"
    rm -f "$EXPECTED_FILES_LIST" "$ACTUAL_FILES_LIST"
    exit 1
else
    echo ":white_check_mark: Success: Valid number of jars detected in the root folder of the agent jar." >> "$GITHUB_STEP_SUMMARY"
    rm -f "$EXPECTED_FILES_LIST" "$ACTUAL_FILES_LIST"
fi

#Find any packages that aren't properly shadowed
EXTRA_PACKAGES=$(find . -type d \( -path ./com/newrelic -o -path ./META-INF/versions/9/com/newrelic -o -path ./META-INF/versions/11/com/newrelic \) -prune -o -iname "*.class" -print)

if [ -n "$EXTRA_PACKAGES" ]; then
    echo ":x: Failure: Non com.newrelic packages found in the agent jar. Check output for list of packages." >> "$GITHUB_STEP_SUMMARY"
    echo "Unexpected packages:"
    echo "$EXTRA_PACKAGES"
    exit 1
else
    echo ":white_check_mark: No additional packages found in the agent jar." >> "$GITHUB_STEP_SUMMARY"
    exit 0
fi