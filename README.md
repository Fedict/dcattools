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

More info can be found in the [Scraper README file](README-SCRAPER.md)

## Enhancer

Various tools for enhancing the harvested metadata.

The various "enhancers" are chained by using a counter in the property name.

E.g.

    be.fedict.dcat.enhancers.1.classname=be.fedict.dcat.enhancers.SparqlUpdate
    be.fedict.dcat.enhancers.1.sparqlfile=B:/datagov/cfg/bxlcity/sparql-cat.txt

    be.fedict.dcat.enhancers.2.classname=be.fedict.dcat.enhancers.SparqlUpdate
    be.fedict.dcat.enhancers.2.sparqlfile=B:/datagov/cfg/bxlcity/sparql-map.txt

Mapping e.g. free text keywords to DCAT themes is typically done by loading
an RDF file with SKOS mapping (using altLabel or exactMatch), performing a
SparqlUpdate and removing the SKOS triples.

### Running enhancer

Invoke with

    # java -jar tools.jar location/of/config.properties

Use -D to set logging level and save the log to a file

    # java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug 
           -Dorg.slf4j.simpleLogger.logFile=enhance.log
           -jar tools.jar location/of/config.properties

The configuration file is a Java properties file.

    # full path to store the RDF backing store (SAIL)
    be.fedict.dcat.enhancers.store=B:/datagov/data/wallonie/enhancer.sail
    # full path to the input file (result of the scraper)
    be.fedict.dcat.enhancers.rdfin=B:/datagov/data/wallonie/wallonie.nt
    # full path to store the result (N-Triples)
    be.fedict.dcat.enhancers.rdfout=B:/datagov/data/wallonie/enhanced.nt

    # full class name of the enhancer implementation
    be.fedict.dcat.enhancers.1.classname=be.fedict.dcat.enhancers.SparqlUpdate
    # full path to the Sparql Update script to load
    be.fedict.dcat.enhancers.1.sparqlfile=B:/datagov/cfg/wallonie/sparql-lang-def.txt

    # full class name of the enhancer implementation
    be.fedict.dcat.enhancers.2.classname=be.fedict.dcat.enhancers.SplitProperty
    # name of the property to split
    be.fedict.dcat.enhancers.2.property=http://purl.org/dc/terms/coverage
    # separator character
    be.fedict.dcat.enhancers.2.separator=,

    # full class name of the enhancer implementation
    be.fedict.dcat.enhancers.3.classname=be.fedict.dcat.enhancers.LoadRDF
    # full path to the RDF file to load
    be.fedict.dcat.enhancers.3.rdffile=B:/datagov/cfg/wallonie/data-contact.ttl

    # full class name of the enhancer implementation
    be.fedict.dcat.enhancers.4.classname=be.fedict.dcat.enhancers.SparqlSelect
    # full path to the Sparql Query script to load
    be.fedict.dcat.enhancers.4.sparqlfile=B:/datagov/cfg/common/select-spatial.txt
    # full path to the export file 
    be.fedict.dcat.enhancers.4.outfile=B:/datagov/data/wallonie/geo-missing.txt


### LoadRDF

Loads an RDF file containing triples (e.g. for "static metadata" like contact info,
or mapping categories with SKOS-files) into the local triple store.

Parameters:

    be.fedict.dcat.enhancers.<counter>.classname=be.fedict.dcat.enhancers.LoadRDF
    be.fedict.dcat.enhancers.<counter>.rdffile=/full/path/to/file.ttl

### SparqlSelect

Performs a Sparql SELECT on the local triple store (e.g. for listing properties).
The output will be stored in a text file.

Parameters:

    be.fedict.dcat.enhancers.<counter>.classname=be.fedict.dcat.enhancers.SparqSelect
    be.fedict.dcat.enhancers.<counter>.sparqlfile=/full/path/to/query.txt
    be.fedict.dcat.enhancers.<counter>.outfile=/full/path/to/output.txt


### SparqlUpdate

Performs a SparqlUpdate (INSERT and/or DELETE) on the local triple store.
Can be combined with LoadRDF to perform a mapping.

Parameters:

    be.fedict.dcat.enhancers.<counter>.classname=be.fedict.dcat.enhancers.SparqUpdate
    be.fedict.dcat.enhancers.<counter>.sparqlfile=/full/path/to/query.txt


### SplitProperty

Split a property into multiple properties (useful when e.g.
a keyword property contains a concatenated list of keywords) 

 
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
