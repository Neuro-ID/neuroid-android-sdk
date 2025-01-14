#!/bin/bash

# Defined build flavors
flavors=("androidLib" "reactNativeLib")

# Iterate over the flavors
for flavor in "${flavors[@]}"
do
    echo "Building and testing $flavor..."

    # Assemble the flavor
    ./gradlew assemble${flavor}Debug

    # If the assemble failed, exit with an error
    if [ $? -ne 0 ]; then
        echo "Assemble failed for $flavor"
        exit 1
    fi
done

echo "All flavors built and tested successfully!"