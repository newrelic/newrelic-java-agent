# rm -rf deps

TESTS="tests"

if [ ! -d "$TESTS" ]; then
  echo "Error: Directory '$TESTS' not found."
  exit 1
fi

for file in "$TESTS"/*/logs/*; do
  if [ -f "$file" ]; then
    # Your code to process the file goes here
    echo "Removing file: $file"
    rm $file
  fi
done

for file in "$TESTS"/*/results/*; do
  if [ -f "$file" ]; then
    # Your code to process the file goes here
    echo "Removing file: $file"
    rm $file
  fi
done