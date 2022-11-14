#!/bin/sh
file="version.properties"

while IFS='=' read -r key value
do
    key=$(echo $key | tr '.' '_')
    eval ${key}=\${value}
done < "$file"

echo "GITHUB_SDK_VERSION=$SDK_VERSION" >> $GITHUB_ENV
