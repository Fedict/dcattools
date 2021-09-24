# Scraper tool

Currently, there are abstract Java classes available for scraping
 * DCAT RDF files
 * CKAN portals, both via the JSON CKAN-API and via RDF rendering
 * OpenDataSoft portals, using the API / DCAT-ish metadata
 * GeoNetwork, CSW and RDF
 * Excel files
 * HTML sites

Adding a new one can be as simple as subclassing `be.fedict.dcat.scrapers.Scraper`, setting a new name and recompiling the scraper module.

## Running

Invoke with

    # java -jar scraper.jar name-of-datasource

Use -D to set logging level and save the log to a file

    # java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug 
           -Dorg.slf4j.simpleLogger.logFile=update.log
           -jar scraper.jar name-of-datasource

## Configuration

The configuration file is a `scraper.properties` file .
Make sure that the (sub)directories exist before running the scraper.

For example:

    # full class name of the scraper implementation
    be.fedict.dcat.scrapers.classname=be.fedict.dcat.scrapers.wallonie.OdsWallonie
    # URL of the site/portal to scrape
    be.fedict.dcat.scrapers.url=http://opendata.digitalwallonia.be
    # default language of the site/portal (nl, fr, de or en)
    be.fedict.dcat.scrapers.deflanguage=fr
    # comma-separated list of the languages on the site/portal (nl fr de en)
    be.fedict.dcat.scrapers.languages=fr

## Proxy

If needed, use the JVM system properties -Dhttp.proxyHost=proxy.host.be -Dhttp.proxyPort=8080

## Delay

Note that, in order to avoid overloading the site/portal being scraped, 
the scrapers sleep about 1 second between HTTP requests.
