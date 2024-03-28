#!/usr/bin/env bash

export TIMESTAMP=$(date -u +'%Y-%m-%d-%H.%M')

cd open-meteo-zodiac && ./build.sh && cd ..

cd docker || exit

# jdk
mkdir -p java
cd java || exit

if [ -d "zulu" ]; then
  echo jdk already present...
else
  if [ -f "zulu21.32.17-ca-jdk21.0.2-linux_x64.zip" ]; then
    echo jdk already downloaded...
  else
    wget https://cdn.azul.com/zulu/bin/zulu21.32.17-ca-jdk21.0.2-linux_x64.zip
  fi

  unzip zulu21.32.17-ca-jdk21.0.2-linux_x64.zip
  mv ./zulu21.32.17-ca-jdk21.0.2-linux_x64 ./zulu
fi

cd ..

docker buildx build  --platform linux/amd64 . -t docker.io/pavelbucekcz/meteodata-zodiac:$TIMESTAMP

cd ..