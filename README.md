# DCAT tools

Various DCAT tools for harvesting metadata from Belgian open data portals, 
converting metadata to DCAT-AP files and updating the Belgian 
[data.gov.be](http://data.gov.be) portal.

The portal itself is a Drupal 7 website, based on Fedict's 
[Openfed](https://drupal.org/project/openfed) distribution plus two extra modules
[RestWS](https://drupal.org/project/restws) and 
[RestWS i18n](https://www.drupal.org/project/restws_i18n).

## Data

Only interested in the result ? The N-Triples file (DCAT-AP-ish) can be found
in [data/datagovbe.nt](data/datagovbe.nt)

## Overview of the tools

![Components](components.png)

### Requirements

These tools can be used with Oracle Java runtime 1.8 (1.7 will probably work, 
but not tested), on a headless machine, i.e. there is no fancy GUI.

Internet connection is obviously required, although a proxy can be used.

Binaries can be found in [dist/bin](dist/bin), compiling from source requires 
the Oracle JDK and Maven.

### Main parts

* Helper classes: for storing scraped pages locally, conversion tools etc.
* Various [scrapers](#scraper): getting metadata from various repositories
and websites, and turning the metadata into DCAT files
* DCAT [enhancers](#enhancer): for improving the DCAT files, 
e.g. map site-specific themes add missing properties
and prepare the files for updating data.gov.be
* Data.gov.be [updater](#updater): update the data.gov.be (Drupal 7) website 
using the enhanced DCAT files
* Some [tools](#tools): link checker

### Configuration

All configuration is done using Java (plain text) properties files.
Some examples can be found in [dist/cfg](dist/cfg)

### Notes

* Based on rdf4j (formerly known as Sesame), MapDB, Guava and other Java open source libraries.
* Logging uses SLF4J.

## Scraper

This command-line Java tool scrapes various websites and CKAN portals.

Each site / portal / file requires a specialized scraper Java class and a
configuration file.

More info can be found in the [Scraper README file](README-SCRAPER.md).

## Enhancer

Various tools for enhancing the harvested metadata.

The various "enhancers" are chained by using a counter in the property name.

Mapping e.g. free text keywords to DCAT themes is typically done by loading
an RDF file with SKOS mapping (using altLabel or exactMatch), performing a
SparqlUpdate and removing the SKOS triples.

More info can be found in the [Enhancers README file](README-ENHANCERS.md).

## Updater

This tool updates the data.gov.be (Drupal 7) site using a REST interface,
provided the Drupal modules RestWS and RestWS_i18n are installed.

### Running updater 

Invoke with

    # java -jar datagovbe.jar location/of/config.properties

Use -D to set logging level and save the log to a file

    # java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug 
           -Dorg.slf4j.simpleLogger.logFile=update.log
           -jar datagovbe.jar location/of/config.properties


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
    

## Tools

A collection of various tools, currently only a simple command line link checker is implemented.
It uses HEAD HTTP requests and pauses between requests (to avoid overloading the server)

Invoke with

    # java -jar tools.jar be.fedict.dcat.tools.LinkChecker location/of/config.properties


Use -D to set logging level and save the log to a file

    # java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug 
           -Dorg.slf4j.simpleLogger.logFile=linkchekcer.log
           -jar tools.jar be.fedict.dcat.tools.LinkChecker 
            location/of/config.properties
