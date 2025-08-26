#! /bin/bash

CHANGED_FILES=$@
MYPATH=$0
INSTALL_DIR="`dirname ${MYPATH}`"
CHECK_TITLE="IMPLEMENTATION_TITLE_SHOULD_MATCH_MODULE_NAME"
CHECK_VERIFY_INSTRUMENTATION="VERIFY_INSTRUMENTATION_SHOULD_CONTAIN_PASSESONLY"


FAILURES=""
for file in $CHANGED_FILES; do
    if [[ $file == *instrumentation/*.gradle ]]; then
        echo "Checking build file: $file"
        /bin/sh "${INSTALL_DIR}/check-verify.sh" "$file"
        if [ $? -ne 0 ]; then
            FAILURES="${FAILURES} ${CHECK_VERIFY_INSTRUMENTATION}:${file} \n"
        fi
        /bin/sh "${INSTALL_DIR}/check-implementation-title.sh" "$file"
        if [ $? -ne 0 ]; then
           FAILURES="${FAILURES} ${CHECK_TITLE}:${file} \n"
        fi
    fi
done
echo "~~~~~~~~~~ RESULTS ~~~~~~~~~~"
if [[ -n "$FAILURES" ]]; then
    echo "Optional build file checks failed at the following locations. Please review for accuracy: "
    echo -e "$FAILURES"
    exit 1
else
    echo "All build files passed lint checks."
fi