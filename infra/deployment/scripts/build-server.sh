#!/bin/bash
set -e
cd ../../server/ && sbt assembly && cd -
cp ../../server/target/scala-2.13/discord-scammer-detector-bot-assembly-1.0-SNAPSHOT.jar app.jar
