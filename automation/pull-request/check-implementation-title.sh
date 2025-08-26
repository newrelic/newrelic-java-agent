#! /bin/bash

FILE=$1

if [ -z "$FILE" ]; then
  echo "Usage: $0 <path-to-gradle-file>"
  exit 1
fi

MODULE_NAME=$(basename "$(dirname "$FILE")")
EXPECTED_TITLE="com.newrelic.instrumentation.$MODULE_NAME"
TITLE_LINE=$(grep "'Implementation-Title':\s*'${EXPECTED_TITLE}'" "$FILE")

#If the line is empty, fail
if [ -z "$TITLE_LINE" ]; then
  echo "  Warning: Expected $EXPECTED_TITLE in 'Implementation-Title' field but did not find it."
  exit 1
fi