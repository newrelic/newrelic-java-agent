for file in logs/*; do
  if [ -f "$file" ]; then
    echo "Removing file: $file"
    rm $file
  fi
done

for file in results/*; do
  if [ -f "$file" ]; then
    echo "Removing file: $file"
    rm $file
  fi
done