# DCAT tools

Various DCAT tools for harvesting metadata from open data portals, 
converting them to DCAT-AP files and updating data.gov.be

## Overview

### Requirements

These tools can be used with Oracle Java 1.8 (1.7 will probably work, but not tested),
on a headless machine, i.e. there is no fancy GUI.

Internet connection is obviously required, a proxy can be used.

### Main parts

* Helper classes: for storing scraped pages locally, conversion tools etc.
* Various [scrapers](#scraper): getting metadata from various repositories en websites, 
and turning into DCAT files
* DCAT enhancers: for improving the DCAT files, e.g. map site-specific themes to EUROVOC, 
add missing properties and prepare the files for updating data.gov.be
* Data.gov.be updater: update the data.gov.be (Drupal 7) website using the enhanced DCAT files

### Notes

* Based on rdf4j (formerly known as Sesame), MapDB, Guava and other open source libraries.
* Logging uses SLF4J.

## Scraper

This command-line Java tool scrapes various websites and CKAN portals.
Each site / portal requires a specialized scraper Java class and a configuration file.

Invoke with

    # java -jar scraper.jar location/of/config.properties

Use -D to set logging level and save the log to a file

    # java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug 
           -Dorg.slf4j.simpleLogger.logFile=update.log
           -jar scrapper.jar location/of/config.properties

The configuration file is a Java properties file.

    # full class name of the scraper implementation
    be.fedict.dcat.scrapers.classname=be.fedict.dcat.scrapers.CkanWallonie
    # full path to the location (MapDB)
    be.fedict.dcat.scrapers.cache=B:/datagov/data/wallonie/cache
    # full path to store the RDF backing store (SAIL)
    be.fedict.dcat.scrapers.store=B:/datagov/data/wallonie/wallonie.sail
    # full path for the result file (N-Triples format)
    be.fedict.dcat.scrapers.rdfout=B:/datagov/data/wallonie/wallonie.nt
    # URL of the site/portal to scrape
    be.fedict.dcat.scrapers.url=http://opendata.digitalwallonia.be
    # default language of the site/portal (nl, fr, de or en)
    be.fedict.dcat.scrapers.deflanguage=fr
    # comma-separated list of the languages on the site/portal (nl fr de en)
    be.fedict.dcat.scrapers.languages=fr

    # optional proxy host and port
    #be.fedict.dcat.scrapers.proxy.host=your.proxy.test
    #be.fedict.dcat.scrapers.proxy.port=888


Note that, in order to avoid overloading the site/portal being scraped, 
the scrapers sleep about 1 second between HTTP requests.

## Enhancer

The enhancers can be chained.

## Updater

This tool updates the data.gov.be (Drupal 7) site using a REST interface,
provided the Drupal modules RestWS and RestWS_i18n are installed.

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
    