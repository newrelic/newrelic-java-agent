#!/bin/bash

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

# Expand the agent jar
unzip -q newrelic.jar

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
    echo ":x: Failure: Extra jars detected in the root folder of the agent jar:" >> "$GITHUB_STEP_SUMMARY"
    diff "$EXPECTED_FILES_LIST" "$ACTUAL_FILES_LIST" >> "$GITHUB_STEP_SUMMARY"
    rm -f "$EXPECTED_FILES_LIST" "$ACTUAL_FILES_LIST"
    exit 1
else
    echo ":white_check_mark: Success: Valid number of jars detected in the root folder of the agent jar." >> "$GITHUB_STEP_SUMMARY"
    rm -f "$EXPECTED_FILES_LIST" "$ACTUAL_FILES_LIST"
    exit 0
fi