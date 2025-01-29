#!/bin/sh

# (only) update Drupal D10 without executing other steps,
#
# Bart Hanssens

# Directories
BIN=$HOME
DATA=/mnt/datagovbe

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

# Update the data.gov.be Drupal portal
# Parameter: project code
update() {
	step $1 "update"

 	USER_PASS=$(echo $SECRETS | grep ^$1)
  	D_USER=$(echo $USER_PASS | cut -d ':' -f 2)
  	D_PASS=$(echo $USER_PASS | cut -d ':' -f 3)
 
	java  -Dorg.slf4j.simpleLogger.defaultLogLevel=info \
 		-Dorg.slf4j.simpleLogger.logFile=$DATA/$1/logs/update.log \
 		-jar uploaderd10.jar \
   		--user=$D_USER \
     		--password=$D_PASS \
   		--url=https://5377.f2w.bosa.be \
     		--file=$DATA/$1/$1-translated.nt

 	status $1 "update" $2 $?
 }
 
# Main

# Parameter: one or more project codes (separated by ',')

IFS=',' 
sources=()
for i in "$1"; do sources+=($i); done

for source in ${sources[@]}; do
 	update $source
done
