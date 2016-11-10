#!/bin/sh

java  -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -Dorg.slf4j.simpleLogger.logFile=scrape-$1.log -jar scrapers.jar  ../cfg/$1/scraper.properties
