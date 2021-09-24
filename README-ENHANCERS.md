# Enhancers / SPARQL scripts

## Scripts.txt file

The scraper jar contains per scraper package one scripts.txt file (stored as a resource).
This file contains the list of additional data files to be loaded and/or additional SPARQL scripts to be run.

E.g. scripts.txt in be.fedict.dcat.scrapers.bipt:

```
bipt/data-publ-contact.ttl
bipt/sparql-publ-contact.qry
bipt/sparql-license.qry
bipt/sparql-theme.qry
sparql-geo-belgium.qry
data-media.ttl
sparql-map-media.qry
clear-skos.qry
```

These files are either located in the scraper package be.fedict.dcat.scrapers.<datasource>, 
or in the be.fedict.dcat.scrapers package (for generic queries / data).

Data are loaded and scripts are executed in the order they are listed in the file.
Files ending with .ttl are considered to be data files, files ending with .qry are SPARQL query files.


## Note on mappings

Mapping e.g. free text keywords to the controlled list of DCAT themes is done by 
loading an RDF file with SKOS mapping (using altLabel or exactMatch), performing a
SparqlUpdate query and removing the SKOS triples afterwards.

DCAT uses several pre-defined controlled vocabularies (e.g. file types, geo...),
but these vocabularies are not directly supported by Drupal / the data.gov.be tools.

Therefore, one has to manually create the taxonomies in Drupal, and map them to 
the URIs of the controlled DCAT vocabularies.

For example:
    
    @prefix skos: <http://www.w3.org/2004/02/skos/core#> .

    <http://data.gov.be/en/taxonomy/term/32>
        skos:exactMatch <http://publications.europa.eu/resource/authority/data-theme/AGRI> ;
        skos:prefLabel "AGRICULTURE, FISHERIES, FORESTRY, FOOD"@en ;
        skos:altLabel  "arbre"@fr, "paysage"@fr .

This indicates that the harvested metadata uses terms like "arbre" and "paysage",
which are both corresponding to the http://data.gov.be/en/taxonomy/term/32 
term on data.gov.be, and also (via skos:exactMatch) to the Agriculture theme
on the EU Data portal.


The same goes for the taxonomy of organizations, geo coverage and file types.
