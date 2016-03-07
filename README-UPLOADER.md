# Uploader tool

## Users

It is good practice to create a separate Drupal user per harvested source.
This has to be done manually by the Drupal admin, and the user must have the
rights to import data using the RestWS module.


## Running uploader

Invoke with

    # java -jar datagovbe.jar location/of/config.properties

Use -D to set logging level and save the log to a file

    # java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug 
           -Dorg.slf4j.simpleLogger.logFile=update.log
           -jar datagovbe.jar location/of/config.properties

## Configuration

The configuration file is a Java properties file.

    # URL of the Drupal 7 website
    be.fedict.datagovbe7.drupal=http://data.gov.be
    # Comma separated list of language codes
    be.fedict.datagovbe7.languages=nl,fr

    # Drupal user ID number
    be.fedict.datagovbe7.userid=1234
    # Drupal user login
    be.fedict.datagovbe7.user=mylogin
    # Drupal user password
    be.fedict.datagovbe7.pass=secret_password

    # Proxy settings, comment out if no proxy is required
    #be.fedict.datagovbe7.proxy.host=your.proxy
    #be.fedict.datagovbe7.proxy.port=8080

    # Local N-Triples DCAT file to be used for updating the site
    be.fedict.datagovbe7.rdfin=B:/datagov/data/statbelpubs/enhanced.nt
    # Temporarily local RDF store that will be used to load the DCAT file 
    be.fedict.datagovbe7.store=B:/datagov/data/statbelpubs/drupal.sail
    