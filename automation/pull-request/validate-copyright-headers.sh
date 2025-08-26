#! /bin/bash

NEWLY_ADDED_FILES=$@
CURRENT_YEAR=$(date +%Y)
EXPECTED_COPYRIGHT_HEADER="Copyright $CURRENT_YEAR New Relic Corporation"


FAILURES=""
for file in $NEWLY_ADDED_FILES; do
    if [[ $file == *.java || $file == *.scala || $file == *.kt ]]; then
        COPYRIGHT_STANZA=$( head -n 10 "$file" | grep "$EXPECTED_COPYRIGHT_HEADER")
        if [ -z "$COPYRIGHT_STANZA" ]; then
          FAILURES="$FAILURES $file \n"
        fi
    fi
done

if [ -n "$FAILURES" ]; then
  echo "ERROR: Some net-new source code did not include the expected Copyright phrase: \"$EXPECTED_COPYRIGHT_HEADER\" ."
  echo "Check that headers are present and up-to-date at the following locations: "
  echo -e "\n$FAILURES"
  exit 1
else
  echo "All new source code contained expected Copyright headers."
fi



