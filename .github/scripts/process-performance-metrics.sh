#!/bin/bash
#
# Process AIT Performance Metrics
#
# This script processes JSONL-format performance metrics files from AIT test runs
# and generates a markdown file with comparison tables.
#
# Arguments:
#   input-dir     Directory containing *-metrics-summary.json files (JSONL format)
#   output-file   Path to output markdown file
#   report-title  Title for the report (e.g., "Release9.0.0")
#
# JSONL Format:
#   Each input file contains one JSON object per line (not a JSON array).
#   Each JSON object represents one test result with metadata and metrics.
#
# Output Format:
#   Markdown with nested structure:
#   - Level 1: Directory (e.g., "server")
#   - Level 2: Test File (e.g., "tomcat")
#   - Level 3: Test Case (e.g., "test_tomcat")
#   Each test case contains a comparison table showing all
#   version combinations.
#

set -euo pipefail

if [ $# -ne 3 ]; then
    echo "ProcessPerformanceMetrics: Incorrect number of arguments" >&2
    echo "ProcessPerformanceMetrics: $0 <input-dir> <output-file> <report-title>" >&2
    exit 1
fi

INPUT_DIR="$1"
OUTPUT_FILE="$2"
REPORT_TITLE="$3"

if [ ! -d "$INPUT_DIR" ]; then
    echo "ProcessPerformanceMetrics: Input directory does not exist: $INPUT_DIR" >&2
    exit 1
fi

if ! command -v jq &> /dev/null; then
    echo "ProcessPerformanceMetrics: jq is required but not installed" >&2
    exit 1
fi

cat > "$OUTPUT_FILE" << EOF
# Performance Metrics - ${REPORT_TITLE}

Generated: $(date)

EOF

TEMP_FILE=$(mktemp)

trap 'rm -f "$TEMP_FILE"' EXIT

FILE_COUNT=0
LINE_COUNT=0

for file in "$INPUT_DIR"/*-metrics-summary.json; do
    if [ -f "$file" ]; then
        FILE_COUNT=$((FILE_COUNT + 1))

        while IFS= read -r test_result; do
            [ -z "$test_result" ] && continue

            LINE_COUNT=$((LINE_COUNT + 1))

            TEST_NAME=$(echo "$test_result" | jq -r '.metadata.test_name')
            JAVA_VERSION=$(echo "$test_result" | jq -r '.metadata.java_version')
            FRAMEWORK_VERSION=$(echo "$test_result" | jq -r '.metadata.framework_version // "N/A"')

            DIR=$(echo "$TEST_NAME" | cut -d'-' -f1)
            REST=$(echo "$TEST_NAME" | cut -d'-' -f2-)
            TEST_FILE=$(echo "$REST" | cut -d'.' -f1)
            TEST_CASE=$(echo "$REST" | cut -d'.' -f2-)

            # dir|test_file|test_case|test_name|java_version|framework_version|json_data
            echo "${DIR}|${TEST_FILE}|${TEST_CASE}|${TEST_NAME}|${JAVA_VERSION}|${FRAMEWORK_VERSION}|${test_result}" >> "$TEMP_FILE"
        done < "$file"
    fi
done

if [ ! -s "$TEMP_FILE" ]; then
    echo "ProcessPerformanceMetrics: No test results found in input directory" >&2
    echo "No test results found." >> "$OUTPUT_FILE"
    exit 0
fi

sort -t'|' -k1,1 -k2,2 -k3,3 -k5,5n -k6,6 "$TEMP_FILE" -o "$TEMP_FILE"

close_test_case() {
    if [ -n "$CURRENT_TEST_CASE" ]; then
        echo "" >> "$OUTPUT_FILE"
        echo "</details>" >> "$OUTPUT_FILE"
        echo "</blockquote>" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
    fi
}

close_test_file() {
    if [ -n "$CURRENT_TEST_FILE" ]; then
        echo "" >> "$OUTPUT_FILE"
        echo "</details>" >> "$OUTPUT_FILE"
        echo "</blockquote>" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
    fi
}

close_directory() {
    if [ -n "$CURRENT_DIR" ]; then
        echo "" >> "$OUTPUT_FILE"
        echo "</details>" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
    fi
}

CURRENT_DIR=""
CURRENT_TEST_FILE=""
CURRENT_TEST_CASE=""

while IFS='|' read -r DIR TEST_FILE TEST_CASE TEST_NAME JAVA_VERSION FRAMEWORK_VERSION JSON_DATA; do

    if [ "$DIR" != "$CURRENT_DIR" ]; then
        close_test_case
        close_test_file
        close_directory

        echo "<details>" >> "$OUTPUT_FILE"
        echo "<summary><strong>📁 ${DIR}</strong></summary>" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
        CURRENT_DIR="$DIR"
        CURRENT_TEST_FILE=""
        CURRENT_TEST_CASE=""
    fi

    if [ "$TEST_FILE" != "$CURRENT_TEST_FILE" ]; then
        close_test_case
        close_test_file

        echo "<blockquote>" >> "$OUTPUT_FILE"
        echo "<details>" >> "$OUTPUT_FILE"
        echo "<summary><strong> ${TEST_FILE}</strong></summary>" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
        CURRENT_TEST_FILE="$TEST_FILE"
        CURRENT_TEST_CASE=""
    fi

    if [ "$TEST_CASE" != "$CURRENT_TEST_CASE" ]; then
        close_test_case

        echo "<blockquote>" >> "$OUTPUT_FILE"
        echo "<details>" >> "$OUTPUT_FILE"
        echo "<summary><strong> ${TEST_CASE}</strong></summary>" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"

        echo "| Java | Framework | CPU Time (s) | Response Time (s) | Response Count | Throughput | Errors | Heap Max (%) |" >> "$OUTPUT_FILE"
        echo "|------|-----------|--------------|-------------------|----------------|------------|--------|--------------|" >> "$OUTPUT_FILE"

        CURRENT_TEST_CASE="$TEST_CASE"
    fi

    CPU_TIME=$(echo "$JSON_DATA" | jq -r '.metrics.cpu_time // "N/A"')
    RESP_TIME=$(echo "$JSON_DATA" | jq -r '.metrics.response_time_total // "N/A"')
    RESP_COUNT=$(echo "$JSON_DATA" | jq -r '.metrics.response_count // "N/A"')
    THROUGHPUT=$(echo "$JSON_DATA" | jq -r '.metrics.throughput // "N/A"')
    ERROR_COUNT=$(echo "$JSON_DATA" | jq -r '.metrics.error_count // "N/A"')
    HEAP_UTIL=$(echo "$JSON_DATA" | jq -r '.metrics.heap_utilization_max // "0.0"')
    HEAP_PRESENT=$(echo "$JSON_DATA" | jq -r '.metrics.heap_metrics_present')

    # formatting values
    CPU_TIME_FMT=$(printf "%.2f" "$CPU_TIME" 2>/dev/null || echo "$CPU_TIME")
    RESP_TIME_FMT=$(printf "%.2f" "$RESP_TIME" 2>/dev/null || echo "$RESP_TIME")
    HEAP_UTIL_FMT=$(printf "%.1f%%" "$HEAP_UTIL" 2>/dev/null || echo "0.0%")
    if [ "$HEAP_PRESENT" = "false" ] || [ "$HEAP_UTIL" = "0.0" ]; then
        HEAP_UTIL_FMT="-"
    fi

    echo "| ${JAVA_VERSION} | ${FRAMEWORK_VERSION} | ${CPU_TIME_FMT} | ${RESP_TIME_FMT} | ${RESP_COUNT} | ${THROUGHPUT} | ${ERROR_COUNT} | ${HEAP_UTIL_FMT} |" >> "$OUTPUT_FILE"

done < "$TEMP_FILE"

close_test_case
close_test_file
close_directory