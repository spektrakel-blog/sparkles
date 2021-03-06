#!/bin/bash
set -e
APP=$1
if [ -z $APP ]
  then echo "usage: $0 [app]"; exit 1
fi

###
# TODO: check if gradle docker plugin can replace this script
# https://github.com/palantir/gradle-docker


SCRIPT_PATH="$( cd "$(dirname "$0")" ; pwd -P )"
pushd $SCRIPT_PATH/..

VERSION="$(git describe --tags --always --first-parent)"
echo "Git version: $VERSION"

echo "Building $APP with gradle..."
./gradlew apps:$APP:clean
./gradlew apps:$APP:assemble

echo "Copying distributions to Docker build context..."
rm -rf tools/docker/gradle-app/dist
mkdir -p tools/docker/gradle-app/dist
cp apps/$APP/build/distributions/*.zip tools/docker/gradle-app/dist

echo "Building the docker container..."
cd tools/docker/gradle-app
docker build \
  --build-arg bin=$APP \
  --build-arg zip=$(basename *.zip .zip) \
  -t sparkles/$APP:latest \
  -t sparkles/$APP:$VERSION \
  .

popd
