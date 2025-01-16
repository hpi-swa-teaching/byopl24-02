#!/bin/sh

FILE="$1"
sed -i '' 's/\[/👉/g' "$FILE"
sed -i '' 's/\]/👈/g' "$FILE"
sed -i '' '/class/s/</🤝/g' "$FILE"
sed -i '' 's/this/self/g' "$FILE"
