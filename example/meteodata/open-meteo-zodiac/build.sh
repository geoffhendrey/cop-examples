#!/usr/bin/env bash

./gradlew clean && ./gradlew build && ./gradlew shadowJar && cp ./build/libs/*-all.jar ../docker/open-meteo-zodiac-0.0.1-all.jar
