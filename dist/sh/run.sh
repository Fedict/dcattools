#!/bin/sh

DATA=/tmp/scraper
BIN=$HOME/bin
SHACL=$HOME/shacl

mkdirs() {
	mkdir -p $DATA/$1/data $DATA/$1/logs $DATA/$1/reports $DATA/$1/status
}

step() {
	echo $2
	echo $2 > $DATA/$1/status/step
}

status() {
	echo $3 > $DATA/$1/status/$2
}

clean() {
	step $1 "clean"

	find $DATA/$1 -type f -exec rm {} \;
}

scrape() {
	step $1 "scrape"

	java -Xmx3G -Dorg.slf4j.simpleLogger.defaultLogLevel=info \
		-Dorg.eclipse.rdf4j.rio.fail_on_sax_non_fatal_errors=false \
		-Dorg.eclipse.rdf4j.rio.fail_on_non_standard_attributes=false \
		-Dorg.slf4j.simpleLogger.logFile=$DATA/$1/logs/scrape.log \
		-jar $BIN/scrapers.jar -n $1
	
	status $1 "scrape" $2 $?
}

validate() {
	step $1 "validate"
	
	java -Dorg.slf4j.simpleLogger.logFile=$BIN/$1/logs/validate.log \
		-jar $BIN/shaclvalidator.jar \
		--data=file:///$DATA/$1/data/$1.nt \
		--shacl=file:///$SHACL/shapes.ttl \
		--shacl=file:///$SHACL/shapes_recommended.ttl \
		--shacl=file:///$SHACL/mdr-vocabularies.shape.ttl \
		--shacl=file:///$SHACL/range.ttl \
		--report=$DATA/$1/reports/report-dcatap3.html \
 		--countClasses --countProperties \
		--countValues=dcat:theme --countValues=dcat:mediaType \
		--countValues=dcterms:format --countValues=dcterms:license \
		--countValues=dcterms:spatial --countValues=dcterms:publisher \
		--countValues=dcterms:creator --countValues=dcterms:contributor \
		--countValues=dcterms:rightsHolder
	
	status $1 "validate" $2 $?
}


if [ ! -d $DATA/$1 ]; then
	mkdirs $1
fi

clean $1
scrape $1
validate $1

