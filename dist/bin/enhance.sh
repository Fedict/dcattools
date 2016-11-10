#!/bin/sh

java -Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dorg.slf4j.simpleLogger.logFile=enhance-$1.log -jar tools.jar  B:/datagov/cfg/$1/enhancer.properties
