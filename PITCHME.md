# Data.gov.be

Aggregating metadata from portals across Belgium

---

## Open Data Portals in Belgium

Various local, regional and federal portals ...
- powered by different software products 
- publishing metadata in different languages
- using different themes and keywords

---

## Overview


---

## How it works

- Scrapers, harvesting metadata from portals 
- Enhancers, cleaning and transforming metadata |
- Upload tool, sends the metadata to Drupal |
- Drupal website [data.gov.be](http://data.gov.be) |
- Full export on [github](https://github.com/fedict/dcat)

+++

### The need for scrapers

> In a perfect world, everyone is using the same format to exchange data.

+++

### Scrapers

- Command-line
- Custom parser per scraped site |
- Written in Java, open source |
  - [JSoup](https://jsoup.org/) HTML parser
  - [RDF4J](http://rdf4j.org/) linked data library

+++

### Enhancers

- Cleaning en enriching the scraped metadata
   - Various small SPARQL queries for small corrections
- Mapping keywords and themes to EU ODP themes |
   - Using (manually created) SKOS files

+++

### Upload tool

- Translates DCAT-AP into Drupal JSON

+++

### Drupal

- Drupal 7 website, based on OpenFed
- Additional modules Rest-WS and Rest-WS i18n
  - Allows updates via JSON REST API
  - Services modules too heavy / overkill

---

## Why not CKAN ?

- Wasn't fully DCAT-AP ready in 2015
- We already have 100+ Drupal websites
- Doesn't solve harvesting metadata from non-CKAN sites

---

## Why not reuse the EU ODP ?

- The code of the EU ODP wasn't available yet in 2015
- 

The data is sent to the ODP anyway

---

## DCAT-AP exchange

- Metadata exchange format (RDF)
  - Titles, descriptions, download links ...
- "Application Profile" of W3C DCAT |
- Promoted by JoinUp.eu / European Commission |

---

## Thank you

Questions ? 

[opendata@belgium.be](mailto:opendata@belgium.be)