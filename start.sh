#!/bin/bash
./gradlew bootJar && docker compose up -d --build

read -p "Press enter to continue..."