# Uploader tool

## Users

It is good practice to create a separate Drupal user per harvested source.
This has to be done manually by the Drupal admin, and the user must have the
rights to import data using the RestWS module.

## Module settings and permissions

Some Drupal modules need to be enabled / disabled:

  * Enable `Basic Authentication`, `RestWS` and `RestWS i18n` modules
  * Disable `Global Redirect` and `Antibot` modules
  * Enable `Bypass content access control` for the `Dataset importer`

## Running uploader

Invoke with

    # java -jar uploader.jar location/of/config.properties

Use -D to set logging level and save the log to a file

    # java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug 
           -Dorg.slf4j.simpleLogger.logFile=update.log
           -jar uploader.jar location/of/config.properties

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

    # Local N-Triples DCAT file to be used for updating the site
    be.fedict.datagovbe7.rdfin=B:/datagov/data/statbelpubs/enhanced.nt
    # Temporarily local RDF store that will be used to load the DCAT file 
    be.fedict.datagovbe7.store=B:/datagov/data/statbelpubs/drupal.sail
 
## Proxy

Use the JVM system properties -Dhttp.proxyHost=proxy.host.be -Dhttp.proxyPort=8080

## Update search index

After (or during) the upload, the Lucene search index has to be updated in order to reflect the changes.
This can be done by the Drupal admin (`Index all remaining content`)


## Undo module settings and permissions

For security reasons, the changes in module settings must be reverted

  * Disable `Basic Authentication`, `RestWS` and `RestWS i18n` modules
  * Enable `Global Redirect` and `Antibot` modules
  * Disable `Bypass content access control` for the `Dataset importer`
