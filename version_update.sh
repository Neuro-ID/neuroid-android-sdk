#!/bin/sh
PR_TITLE=$1
echo "${PR_TITLE}"
version=$(echo "$PR_TITLE" | sed 's/\([[A-Z]*-[0-9]*] v\)//g')
VERSION_MAJOR=${version:0:1}
VERSION_MINOR=${version:2:1}
VERSION_PATCH=${version:4:1}

VERSION_PROPERTIES=version.properties
# replace version
sed -i "" "s/VERSION_MAJOR=*[0-9]*/VERSION_MAJOR=${VERSION_MAJOR}/" ${VERSION_PROPERTIES}
sed -i "" "s/VERSION_MINOR=*[0-9]*/VERSION_MINOR=${VERSION_MINOR}/" ${VERSION_PROPERTIES}
sed -i "" "s/VERSION_PATCH=*[0-9]*/VERSION_PATCH=${VERSION_PATCH}/" ${VERSION_PROPERTIES}
