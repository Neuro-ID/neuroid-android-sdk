set -euo pipefail

main() {
  local properties
  local result

  path="version.properties"
  properties="$1"

  echo "path to properties-file: $path"
  echo "property keys: $properties"

  for key in $properties; do
    # For lines that have the given property on the left-hand side, remove
    # the property name, the equals and any spaces to get the property value.
    result=$(sed -n "/^[[:space:]]*$key[[:space:]]*=[[:space:]]*/s/^[[:space:]]*$key[[:space:]]*=[[:space:]]*//p" "$path")

    echo "value of '$key': $result"
    # shellcheck disable=SC2001
    echo "$(echo "$key" | sed 's/[^A-Za-z0-9_]/-/g')=$result" >> $GITHUB_OUTPUT
  done
}
