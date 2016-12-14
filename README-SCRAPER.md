# Scraper tool

Currently, there are abstract Java classes available for scraping
 * CKAN portals, both via the JSON CKAN-API and via RDF rendering
 * OpenDataSoft portals, using the API / DCAT-ish metadata
 * Excel files
 * HTML sites

## Running

Invoke with

    # java -jar scraper.jar location/of/config.properties

Use -D to set logging level and save the log to a file

    # java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug 
           -Dorg.slf4j.simpleLogger.logFile=update.log
           -jar scrapper.jar location/of/config.properties

## Configuration

The configuration file is a Java properties file.
Make sure that the (sub)directories exist before running the scraper.

    # full class name of the scraper implementation
    be.fedict.dcat.scrapers.classname=be.fedict.dcat.scrapers.CkanWallonie
    # full path to the location of the local download cache (MapDB)
    be.fedict.dcat.scrapers.cache=B:/datagov/data/wallonie/cache.mdb
    # full path to the RDF backing store (SAIL)
    be.fedict.dcat.scrapers.store=B:/datagov/data/wallonie/wallonie.sail
    # full path for the result file (N-Triples)
    be.fedict.dcat.scrapers.rdfout=B:/datagov/data/wallonie/wallonie.nt
    # URL of the site/portal to scrape
    be.fedict.dcat.scrapers.url=http://opendata.digitalwallonia.be
    # default language of the site/portal (nl, fr, de or en)
    be.fedict.dcat.scrapers.deflanguage=fr
    # comma-separated list of the languages on the site/portal (nl fr de en)
    be.fedict.dcat.scrapers.languages=fr

## Proxy

Use the JVM system properties -Dhttp.proxyHost=proxy.host.be -Dhttp.proxyPort=8080

## Delay

Note that, in order to avoid overloading the site/portal being scraped, 
the scrapers sleep about 1 second between HTTP requests.
