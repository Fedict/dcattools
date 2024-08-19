#!/bin/sh

# Data.gov.be "glue" script to run different steps (scraping, validating...) in metadata collection.
#
# Bart Hanssens

# Directories
BIN=$HOME
SHACL=$HOME/shacl
DATA=/mnt/datagovbe

# Create directories (if not yet present)
makedirs() {
	mkdir -p $DATA/$1/data $DATA/$1/logs $DATA/$1/reports $DATA/$1/status
}

# record the step in the process being executed
# Parameters: project code, name of the step 
step() {
	echo $1 $2
	echo $2 > $DATA/$1/status/step
}

# record exit status of the step that was executed
# Parameters: project code, name of the step, exit code
status() {
	echo $3 > $DATA/$1/status/$2
}

# remove data from previous run
# Parameter: project code
clean() {
	step $1 "clean"

	rm $DATA/$1/data/$1.nt
	rm $DATA/$1/logs/*
	rm $DATA/$1/reports/*
}

# Scrape metadata from an external source and save it as triples
# Parameter: project code
scrape() {
	step $1 "scrape"

	java -Xmx3G -Dorg.slf4j.simpleLogger.defaultLogLevel=info \
		-Dorg.eclipse.rdf4j.rio.fail_on_sax_non_fatal_errors=false \
		-Dorg.eclipse.rdf4j.rio.fail_on_non_standard_attributes=false \
		-Dorg.slf4j.simpleLogger.logFile=$DATA/$1/logs/scrape.log \
		-jar $BIN/scrapers.jar -n $1
	
	status $1 "scrape" $2 $?
}

# Create SHACL validation reports
# Parameter: project code
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

# Create SHACL validation reports
# Parameter: project code
translate() {
	step $1 "translate"
 }

# Main

# Parameter: one or more project codes (separated by ',')

IFS=',' 
sources=()
for i in "$1"; do sources+=($i); done

for source in ${sources[@]}; do
	if [ ! -d $DATA/$source ]; then
		makedirs $source
	fi

	clean $source
	scrape $source
	validate $source
	translate $source
done
