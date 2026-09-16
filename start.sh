#!/bin/bash
./gradlew clean bootJar && docker compose build --no-cache && docker compose up -d