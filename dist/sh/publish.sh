#!/bin/sh

# Data.gov.be "glue" script to publish the metadata to github (for the European Data Portal).
#
# Bart Hanssens

# Directories
BIN=$HOME
SHACL=$HOME/shacl
DATA=/mnt/datagovbe

# e-translation
E_USER=$ETRANSLATE_USER
E_PASS=$ETRANSLATE_PASS
E_URL=$ETRANSLATE_URL

# Create directories (if not yet present)
makedirs() {
	mkdir -p $DATA/$1 $DATA/$1/logs $DATA/$1/reports $DATA/$1/status
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
	echo $(date -Iseconds) $2 $3 >> $DATA/$1/status/exit
}

# remove data from previous run
# Parameter: project code
clean() {
	step $1 "clean"

	rm $DATA/$1/*.nt
  rm $DATA/$1/*.xml
  rm $DATA/$1/*.gz

 	rm $DATA/$1/cache
	rm $DATA/$1/logs/*
	rm $DATA/$1/reports/*
	rm $DATA/$1/status/*
}

# Scrape metadata from an external source and save it as triples
# Parameter: project code
scrape() {
	step $1 "scrape"

	java -Xmx3G -Dorg.slf4j.simpleLogger.defaultLogLevel=info \
		-Dorg.eclipse.rdf4j.rio.fail_on_sax_non_fatal_errors=false \
		-Dorg.eclipse.rdf4j.rio.fail_on_non_standard_attributes=false \
		-Dorg.slf4j.simpleLogger.logFile=$DATA/$1/logs/scrape.log \
		-jar $BIN/scrapers.jar \
		--dir=$DATA/$1 \
		--name=$1
	
	status $1 "scrape" $2 $?
}

# Create SHACL validation reports
# Parameter: project code
validate() {
	step $1 "validate"
	
	java -Dorg.slf4j.simpleLogger.logFile=$DATA/$1/logs/validate.log \
		-jar $BIN/shaclvalidator.jar \
		--data=file:///$DATA/$1/$1.nt \
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

# Convert to XML
# Parameter: project code
convert() {
  step $1 "convert"

  mv $DATA/$1/$1.nt $DATA/$1/datagovbe.nt

  java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug \
    -Dorg.slf4j.simpleLogger.logFile=$DATA/$1/logs/edp.log \
    -jar $BIN/tools.jar  \
    $DATA/$1/datagovbe.nt \
    $DATA/$1/datagovbe_edp.xml

  res=$?  
  status $1 "convert" $2 $exit

  return $res
}

# Compress files
# Parameter: project code
compress() {
  step $1 "compress"

  gzip -9 $DATA/$1/datagovbe.nt
  gzip -9 $DATA/$1/datagovbe_edp.xml
  
  status $1 "compress" $2 $?
} 

# Publish to github
# Parameter: project code
publish() {
  step $1 "publish"

  status $1 "publish" $2 $?
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
  convert $source

  if [ $? -eq 0 ]; then
    compress $source
    publish $source
  fi
done