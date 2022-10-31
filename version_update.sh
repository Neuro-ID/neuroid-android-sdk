#!/bin/sh
version=`echo $1 | sed 's/^.*[^0-9]\([0-9]*\.[0-9]*\.[0-9]*\).*$/\1/'`
VERSION_MAJOR=$(echo "${version}" | cut -d "." -f 1)
VERSION_MINOR=$(echo "${version}" | cut -d "." -f 2)
VERSION_PATCH=$(echo "${version}" | cut -d "." -f 3)

VERSION_PROPERTIES=version.properties
# replace version
sed -i "s/SDK_VERSION=.*/SDK_VERSION=${version}/" ${VERSION_PROPERTIES}
sed -i "s/VERSION_MAJOR=*[0-9]*/VERSION_MAJOR=${VERSION_MAJOR}/" ${VERSION_PROPERTIES}
sed -i "s/VERSION_MINOR=*[0-9]*/VERSION_MINOR=${VERSION_MINOR}/" ${VERSION_PROPERTIES}
sed -i "s/VERSION_PATCH=*[0-9]*/VERSION_PATCH=${VERSION_PATCH}/" ${VERSION_PROPERTIES}