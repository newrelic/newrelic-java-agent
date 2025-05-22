for file in tests/*/logs/*; do
  if [ -f "$file" ]; then
    echo "Removing file: $file"
    rm "$file"
  fi
  if [ -d "$file" ]; then
      echo "Removing directory: $file"
      rm -rf "$file"
    fi
done

for file in tests/*/results/*; do
  if [ -f "$file" ]; then
    echo "Removing file: $file"
    rm "$file"
  fi
  if [ -d "$file" ]; then
      echo "Removing directory: $file"
      rm -rf "$file"
    fi
done