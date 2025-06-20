for file in tests/*/*; do
  FILE_NAME=$(basename "$file")
  if [[  "${FILE_NAME}" == "results" ]]; then
    echo "Removing results"
    rm -rf "$file"
  fi

  if [[ "${FILE_NAME}" == "logs" ]]; then
    echo "Removing logs"
    rm -rf "$file"
  fi

  if [[ "${FILE_NAME}" == "tmp" ]]; then
    echo "Removing tmp"
    rm -rf "$file"
  fi
done