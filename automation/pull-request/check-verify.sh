#! /bin/bash

FILE=$1

if [ -z "$FILE" ]; then
  echo "Usage: $0 <path-to-gradle-file>"
  exit 1
fi

# Extract verifyInstrumentation block
block=$(awk '/verifyInstrumentation[[:space:]]*\{/,/\}/' "$FILE")

if [ -z "$block" ]; then
  echo "  Warning: verifyInstrumentation block not found. All non-JRE modules should include a verifyInstrumentation block."
  exit 1
fi

if !(echo "$block" | grep -q 'passesOnly') ; then
  echo "  Warning: 'passesOnly' not found inside verifyInstrumentation block. When possible, prefer 'passesOnly' over 'passes'."
  exit 1
fi