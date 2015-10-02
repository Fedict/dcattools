# DCAT tools

Various DCAT tools for harvesting metadata from open data portals, 
converting them to DCAT-AP files and updating data.gov.be

## Requirements

These tools can be used with Oracle Java 1.8 (1.7 will probably work, but not tested),
on a headless machine, i.e. there is no fancy GUI.

Internet connection is obviously required, a proxy can be used.

## Main parts

* Helpers: for storing scraped pages locally, conversion tools etc
* Various scrapers: getting metadata from various repositories en websites, 
and turning into DCAT files
* DCAT enhancers: for improving the DCAT files, e.g. map site-specific themes to EUROVOC, 
add missing properties
* Data.gov.be updater: update the data.gov.be website using the enhanced DCAT files

## Other

Based on rdf4j (formerly known as Sesame)