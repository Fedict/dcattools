# Notes

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

## Uploader

This tool updates the data.gov.be (Drupal 7) site using a REST interface,
provided the Drupal modules RestWS and RestWS_i18n are installed.

More info can be found in the [Uploader README file](README-UPLOADER.md).

## Tools - EDP

Converts DCAT-AP export to the XML format used by the European Data Portal.

Invoke with

    java -jar tools.jar be.gov.data.tools.EDP path/in/datagovbe.nt path/out/edp.xml


## Tools - Linkchecker

Currently only a simple command line link checker is implemented.
It uses HEAD HTTP requests and pauses between requests (to avoid overloading the server)

Invoke with

    java -jar tools.jar be.gov.data.tools.LinkChecker location/of/config.properties


Use -D to set logging level and save the log to a file

    java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug 
         -Dorg.slf4j.simpleLogger.logFile=linkchekcer.log
         -jar tools.jar be.gov.data.tools.LinkChecker location/of/config.properties
