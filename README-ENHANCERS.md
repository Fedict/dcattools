# Enhancers

## Chaining

The various "enhancers" are chained by using a counter in the property name.

E.g.

    be.fedict.dcat.enhancers.1.classname=be.fedict.dcat.enhancers.SparqlUpdate
    be.fedict.dcat.enhancers.1.sparqlfile=B:/datagov/cfg/bxlcity/sparql-cat.txt

    be.fedict.dcat.enhancers.2.classname=be.fedict.dcat.enhancers.SparqlUpdate
    be.fedict.dcat.enhancers.2.sparqlfile=B:/datagov/cfg/bxlcity/sparql-map.txt

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


## Overview of available tools

### CharRecoder

Re-codes literals to UTF8.
Some harvested data claims to be in UTF-8, while ISO-8859 is used.

Parameter:

    be.fedict.dcat.enhancers.<counter>.classname=be.fedict.dcat.enhancers.CharRecoder

### EscapeURI

Converts whitespace in object URIs to "+20"

Parameters:

    be.fedict.dcat.enhancers.<counter>.classname=be.fedict.dcat.enhancers.EscapeURI
    be.fedict.dcat.enhancers.<counter>.property=http://rdf.property#tocheck

### LoadRDF

Loads an RDF file containing triples (e.g. for "static metadata" like contact info,
or mapping categories with SKOS-files) into the local triple store.

Parameters:

    be.fedict.dcat.enhancers.<counter>.classname=be.fedict.dcat.enhancers.LoadRDF
    be.fedict.dcat.enhancers.<counter>.rdffile=/full/path/to/file.ttl

### Skolemizer

Replaces blank nodes with "dummy" Skolem IRIs.
Useful when additional SPARQL updates etc are needed, since not all SPARQL operations
support blank nodes.


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

Split a property into multiple instance of the same property (useful when e.g. 
a keyword property  contains a concatenated list of keywords) 

    be.fedict.dcat.enhancers.<counter>.classname=be.fedict.dcat.enhancers.SlitProperty
    be.fedict.dcat.enhancers.<counter>.property=http://rdf.property#tosplit
    be.fedict.dcat.enhancers.<counter>.separator=,


## Running

Invoke with

    # java -jar tools.jar location/of/config.properties

Use -D to set logging level and save the log to a file

    # java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug 
           -Dorg.slf4j.simpleLogger.logFile=enhance.log
           -jar tools.jar location/of/config.properties

## Configuration

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
