/*
 * Copyright (c) 2016, FPS BOSA DG DT
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package be.gov.data.tools;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;

import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.Serializer.Property;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.ValidatingValueFactory;
import org.eclipse.rdf4j.model.util.Values;

import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.model.vocabulary.ORG;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.XMLParserSettings;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParserSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts to a "nice" XML serialization, for the European Data Portal and other users.
 *
 * @author Bart Hanssens
 */
public class EDP {

	private final static Logger LOG = LoggerFactory.getLogger(EDP.class);

	private final static String ANYURI = "http://www.w3.org/2001/XMLSchema#anyURI";

	private final static String BASE_URI = "http://base.data.gov.be";

	private final static IRI ADMS_IDENTIFIER = Values.iri("http://www.w3.org/ns/adms#identifier");
	private final static IRI ADMS_SAMPLE = Values.iri("http://www.w3.org/ns/adms#sample");
	private final static IRI ADMS_STATUS = Values.iri("http://www.w3.org/ns/adms#status");
	
	private final static IRI DCATAP_AVAILABILITY = Values.iri("http://data.europa.eu/r5r/availability");
	private final static IRI DCATAP_CONFORMS = Values.iri("http://data.europa.eu/r5r/applicableLegislation");
	private final static IRI DCATAP_HVDCAT = Values.iri("http://data.europa.eu/r5r/hvdCategory");
	private final static IRI ELI_RESOURCE = Values.iri("http://data.europa.eu/eli/ontology#LegalResource");
	
	private final static IRI GEO_CUSTODIAN = Values.iri("http://data.europa.eu/930/custodian");
	private final static IRI GEO_DISTRIBUTOR = Values.iri("http://data.europa.eu/930/distributor");
	private final static IRI GEO_ORIGINATOR = Values.iri("http://data.europa.eu/930/originator");
	private final static IRI GEO_PROCESSOR = Values.iri("http://data.europa.eu/930/processor");	
	private final static Set<IRI> CONCEPTS = new HashSet<>(250);

	private final static Set<String> IDENTIFIERS = new HashSet<>(25000);

	/**
	 * Write XML namespace prefixes
	 *
	 * @param w writer
	 * @throws XMLStreamException
	 */
	private static void writePrefixes(XMLStreamWriter w) throws XMLStreamException {
		w.writeNamespace("adms", "http://www.w3.org/ns/adms#");
		w.writeNamespace(DCAT.PREFIX, DCAT.NAMESPACE);
		w.writeNamespace("dcatap", "http://data.europa.eu/r5r/");
		w.writeNamespace("dct", DCTERMS.NAMESPACE);
		w.writeNamespace("eli", "http://data.europa.eu/eli/ontology#");
		w.writeNamespace(FOAF.PREFIX, FOAF.NAMESPACE);
		w.writeNamespace(GEO.PREFIX, GEO.NAMESPACE);
		w.writeNamespace("dcatap", "http://data.europa.eu/r5r/");
		w.writeNamespace("geodcatap", "http://data.europa.eu/930/");
		w.writeNamespace(ORG.PREFIX, ORG.NAMESPACE);
		w.writeNamespace(OWL.PREFIX, OWL.NAMESPACE);
		w.writeNamespace(PROV.PREFIX, PROV.NAMESPACE);
		w.writeNamespace(RDF.PREFIX, RDF.NAMESPACE);
		w.writeNamespace(RDFS.PREFIX, RDFS.NAMESPACE);
		w.writeNamespace(SKOS.PREFIX, SKOS.NAMESPACE);
		w.writeNamespace(VCARD4.PREFIX, VCARD4.NAMESPACE);
		w.writeNamespace(XSD.PREFIX, XSD.NAMESPACE);
	}

	/**
	 * Add to the list of SKOS concepts
	 * 
	 * @param iri 
	 */
	private static void addSkosConcept(IRI iri) {
		if (iri != null && !iri.stringValue().contains(".well-known")) {
			CONCEPTS.add(iri);
		}
	}
	/**
	 * Write literal (date, anyURI, string...) to XML file
	 *
	 * @param w XML writer
	 * @param el element name
	 * @param val value
	 * @throws XMLStreamException
	 */
	private static void writeLiteral(XMLStreamWriter w, String el, Value val)
		throws XMLStreamException {
		if (val instanceof Literal) {
			w.writeStartElement(el);
			// write language or datatype
			String lang = ((Literal) val).getLanguage().orElse("");
			if (!lang.isEmpty()) {
				w.writeAttribute("xml:lang", lang);
			} else {
				IRI dtype = ((Literal) val).getDatatype();
				if ((dtype != null) && !dtype.equals(XSD.STRING)) {
					w.writeAttribute("rdf:datatype", dtype.toString());
				}
			}
			String str = val.stringValue();
			if (str.contains("<")) {
				w.writeCData(str);
			} else {
				w.writeCharacters(str);
			}
			w.writeEndElement();
		}
		if (val instanceof IRI) {
			w.writeStartElement(el);
			w.writeAttribute("rdf:datatype", ANYURI);
			w.writeCharacters(val.stringValue());
			w.writeEndElement();
		}
	}

	/**
	 * Write generic info on (nested) elements
	 * 
	 * @param w
	 * @param con
	 * @param iri 
	 */
	private static void writeGenericInfo(XMLStreamWriter w, RepositoryConnection con, IRI iri) throws XMLStreamException {
		writeReferences(w, con, iri, DCTERMS.TYPE, "dct:type");
		writeLiterals(w, con, iri, DCTERMS.TITLE, "dct:title");
		writeLiterals(w, con, iri, DCTERMS.DESCRIPTION, "dct:description");
		writeLiterals(w, con, iri, RDFS.LABEL, "rdfs:label");
		writeLiterals(w, con, iri, SKOS.NOTATION, "skos:notation");
		writeLiterals(w, con, iri, DCAT.BBOX, "dcat:bbox");
	}
	
	/**
	 * Write multiple literals
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the dataset
	 * @param pred RDF predicate
	 * @param el element name
	 * @throws XMLStreamException
	 */
	private static void writeLiterals(XMLStreamWriter w, RepositoryConnection con,
		Resource uri, IRI pred, String el) throws XMLStreamException {
		try (RepositoryResult<Statement> res = con.getStatements(uri, pred, null)) {
			while (res.hasNext()) {
				writeLiteral(w, el, res.next().getObject());
			}
		}
	}

	/**
	 * Write temporal date info for a dcat:Dataset
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri dataset URI
	 * @throws XMLStreamException
	 */
	private static void writeDates(XMLStreamWriter w, RepositoryConnection con, IRI uri)
		throws XMLStreamException {
		try (RepositoryResult<Statement> res = con.getStatements(uri, DCTERMS.TEMPORAL, null)) {
			while (res.hasNext()) {
				Value v = res.next().getObject();
				if (v instanceof IRI date) {
					w.writeStartElement("dct:temporal");
					w.writeStartElement("dct:PeriodOfTime");
					writeLiterals(w, con, date, DCAT.START_DATE, "dcat:startDate");
					writeLiterals(w, con, date, DCAT.END_DATE, "dcat:endDate");
					w.writeEndElement();
					w.writeEndElement();
				} else {
					LOG.error("Not a date IRI {}", v.stringValue());
				}
			}
		}
	}


	/**
	 * Write multiple contact points of a dcat:Dataset
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the dataset
	 * @param pred RDF predicate
	 * @throws XMLStreamException
	 */
	private static void writeContacts(XMLStreamWriter w, RepositoryConnection con,
		IRI uri, IRI pred) throws XMLStreamException {
		try (RepositoryResult<Statement> res = con.getStatements(uri, pred, null)) {
			while (res.hasNext()) {
				Value v = res.next().getObject();
				if (v instanceof IRI) {
					IRI contact = (IRI) v;
					w.writeStartElement("dcat:contactPoint");
					w.writeStartElement("vcard:Kind");
					writeLiterals(w, con, contact, VCARD4.FN, "vcard:fn");
					writeLiterals(w, con, contact, VCARD4.ORGANIZATION_NAME, "vcard:organization-name");					
					writeReferences(w, con, contact, VCARD4.HAS_URL, "vcard:hasURL");
					writeReferences(w, con, contact, VCARD4.HAS_EMAIL, "vcard:hasEmail");
					writeReferences(w, con, contact, VCARD4.HAS_TELEPHONE, "vcard:hasTelephone");
					w.writeEndElement();
					w.writeEndElement();
				} else {
					LOG.error("Not a contact IRI {}", v.stringValue());
				}
			}
		}
	}

	/**
	 * Write qualified roles of a dcat:Dataset
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the dataset
	 * @param pred RDF predicate
	 * @throws XMLStreamException
	 */
	private static void writeProvenances(XMLStreamWriter w, RepositoryConnection con,
		IRI uri, IRI pred) throws XMLStreamException {
		try (RepositoryResult<Statement> res = con.getStatements(uri, pred, null)) {
			while (res.hasNext()) {
				w.writeStartElement("prov:qualifiedAttribution");
				w.writeStartElement("prov:Attribution");
				
				Resource attrib = (Resource) res.next().getObject();
				try (RepositoryResult<Statement> res2 = con.getStatements(attrib, PROV.AGENT_PROP, null)) {
					while(res2.hasNext()) {
						w.writeStartElement("prov:agent");		
						Resource agent = (Resource) res2.next().getObject();
						writeAgent(w, con, agent);
						w.writeEndElement();
					}
					writeReferences(w, con, attrib, DCAT.HAD_ROLE, "dcat:hadRole", "dcat:Role", false);
				}
				
				w.writeEndElement();
				w.writeEndElement();
			}
		}
	}

	/**
	 * Write geodcat roles of a dcat:Dataset
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the dataset
	 * @param pred RDF predicate
	 * @throws XMLStreamException
	 */
	private static void writeRole(XMLStreamWriter w, RepositoryConnection con,
		IRI uri, IRI pred, String el) throws XMLStreamException {
		try (RepositoryResult<Statement> res = con.getStatements(uri, pred, null)) {
			while (res.hasNext()) {
				Resource agent = (Resource) res.next().getObject();
				w.writeStartElement(el);
				writeAgent(w, con, agent);
				w.writeEndElement();
			}
		}
	}

	/**
	 * Write RDF reference
	 *
	 * @param w XML writer
	 * @param el element name
	 * @param uri ID
	 * @throws XMLStreamException
	 */
	private static void writeReference(XMLStreamWriter w, String el, Value uri) throws XMLStreamException {
		if (uri instanceof IRI iri) {
			w.writeEmptyElement(el);
			w.writeAttribute("rdf:resource", iri.stringValue());
		} else {
			LOG.error("Element {}: not a reference IRI {}", el, uri.stringValue());
		}
	}

	/**
	 * Write multiple references
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the dataset
	 * @param pred RDF predicate
	 * @param el element name
	 * @throws XMLStreamException
	 */
	private static void writeReferences(XMLStreamWriter w, RepositoryConnection con, Resource uri, IRI pred, 
			String el) throws XMLStreamException {
		try (RepositoryResult<Statement> res = con.getStatements(uri, pred, null)) {
			while (res.hasNext()) {
				writeReference(w, el, res.next().getObject());
			}
		}
	}

	/**
	 * Write references and their wrapper class
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the dataset
	 * @param pred RDF predicate
	 * @param el element name
	 * @param classWrap rdf wrapper class 
	 * @throws XMLStreamException
	 */
	private static void writeReferences(XMLStreamWriter w, RepositoryConnection con,
		Resource uri, IRI pred, String el, String classWrap, boolean concept) throws XMLStreamException {
		try (RepositoryResult<Statement> res = con.getStatements(uri, pred, null)) {
			while (res.hasNext()) {
				Value refUri = res.next().getObject();
			//	if (refUri instanceof IRI && refUri.toString().contains("well-known")) {
			//		break;
			//	}
				w.writeStartElement(el);

				if (refUri instanceof IRI) {
					IRI iri = (IRI) refUri;
					String str = iri.stringValue();
					if( !str.contains(".well-known")) {
						if(str.startsWith("http:") || str.startsWith("https:") || str.startsWith("ftp:") || 
								str.startsWith("mailto:") || str.startsWith("tel:")) {
							w.writeEmptyElement(classWrap);
							if (concept) {
								addSkosConcept(iri);
							}
							w.writeAttribute("rdf:about", str);
						} else {
							LOG.error("Not a valid IRI {}", str);
						}
					} else {
						if (classWrap.equals("foaf:Agent")) {
							writeAgent(w, con, iri);
						} else {
							w.writeStartElement(classWrap);
							writeGenericInfo(w, con, iri);
							w.writeEndElement();
						}
					}
				} else {
					w.writeEmptyElement(classWrap);
					w.writeAttribute("rdf:value", refUri.stringValue());
				}
				w.writeEndElement();
			}
		}
	}

	/**
	 * Write (additional) RDF type
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the dataset
	 * @param type RDF type
	 * @throws XMLStreamException
	 */
	private static void writeType(XMLStreamWriter w, RepositoryConnection con,  Resource iri, IRI type) 
		throws XMLStreamException {
		if (con.hasStatement(iri, RDF.TYPE, type, true)) {
			w.writeEmptyElement("rdf:type");
			w.writeAttribute("rdf:resource", type.stringValue());
		}
	}

	/**
	 * Write generic metadata
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the dataset
	 * @throws XMLStreamException
	 */
	private static void writeGeneric(XMLStreamWriter w, RepositoryConnection con,
		IRI uri) throws XMLStreamException {
		writeReferences(w, con, uri, DCTERMS.LANGUAGE, "dct:language");
		writeLiterals(w, con, uri, DCTERMS.IDENTIFIER, "dct:identifier");
		writeLiterals(w, con, uri, DCTERMS.TITLE, "dct:title");
		writeLiterals(w, con, uri, DCTERMS.ALTERNATIVE, "dct:alternative");
		writeLiterals(w, con, uri, DCTERMS.DESCRIPTION, "dct:description");
		writeLiterals(w, con, uri, DCTERMS.CREATED, "dct:created");
		writeLiterals(w, con, uri, DCTERMS.ISSUED, "dct:issued");
		writeLiterals(w, con, uri, DCTERMS.MODIFIED, "dct:modified");
		writeReferences(w, con, uri, ADMS_IDENTIFIER, "adms:identifier", "adms:Identifier", false);
		writeReferences(w, con, uri, ADMS_STATUS, "adms:status");
		writeReferences(w, con, uri, DCTERMS.REFERENCES, "dct:references");
		writeReferences(w, con, uri, DCTERMS.IS_REFERENCED_BY, "dct:isReferencedBy");
		writeReferences(w, con, uri, DCTERMS.PUBLISHER, "dct:publisher");
		writeReferences(w, con, uri, DCTERMS.CREATOR, "dct:creator", "foaf:Agent", false);
		writeReferences(w, con, uri, DCTERMS.CONTRIBUTOR, "dct:contributor");
		writeReferences(w, con, uri, DCTERMS.RIGHTS_HOLDER, "dct:rightsHolder");
		writeReferences(w, con, uri, DCTERMS.CONFORMS_TO, "dct:conformsTo");
		writeReferences(w, con, uri, DCTERMS.ACCESS_RIGHTS, "dct:accessRights");
		writeReferences(w, con, uri, DCATAP_CONFORMS, "dcatap:applicableLegislation", "eli:LegalResource", false);
		writeReferences(w, con, uri, DCATAP_HVDCAT, "dcatap:hvdCategory");
		
//		writeReferences(w, con, uri, DCTERMS.RIGHTS, "dct:rights", "dct:RightsStatement", false);
		writeLiterals(w, con, uri, DCAT.SPATIAL_RESOLUTION_IN_METERS, "dcat:spatialResolutionInMeters");
		writeLiterals(w, con, uri, DCAT.TEMPORAL_RESOLUTION, "dcat:temporalResolution");
	}

	/**
	 * Write dcat distribution
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the dataset
	 * @throws XMLStreamException
	 */
	private static void writeDist(XMLStreamWriter w, RepositoryConnection con,
		IRI uri) throws XMLStreamException {
		w.writeStartElement("dcat:Distribution");
		w.writeAttribute("rdf:about", uri.stringValue());

		writeGeneric(w, con, uri);

		writeReferences(w, con, uri, FOAF.PAGE, "foaf:Page", "foaf:Document", false);
		writeReferences(w, con, uri, DCAT.MEDIA_TYPE, "dcat:mediaType");
		writeReferences(w, con, uri, DCTERMS.FORMAT, "dct:format");
		writeReferences(w, con, uri, DCAT.COMPRESS_FORMAT, "dcat:compressFormat");
		writeReferences(w, con, uri, DCAT.PACKAGE_FORMAT, "dcat:packageFormat");

		// write as anyURI string
		writeReferences(w, con, uri, DCAT.ACCESS_URL, "dcat:accessURL", "foaf:Document", false);
		writeReferences(w, con, uri, DCAT.ACCESS_SERVICE, "dcat:accessService");
		writeReferences(w, con, uri, DCAT.DOWNLOAD_URL, "dcat:downloadURL", "foaf:Document", false);

		writeReferences(w, con, uri, DCTERMS.LICENSE, "dct:license");

		writeReferences(w, con, uri, DCATAP_AVAILABILITY, "dcatap:availability");
		writeLiterals(w, con, uri, DCAT.BYTE_SIZE, "dcat:byteSize");
		writeLiterals(w, con, uri, DCAT.SPATIAL_RESOLUTION_IN_METERS, "dcat:spatialResolutionInMeters");
		writeLiterals(w, con, uri, DCAT.TEMPORAL_RESOLUTION, "dcat:temporalResolution");
		
		w.writeEndElement();
	}

	/**
	 * Write DCAT dataset
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param cl class name (used for element tag)
	 * @param uri URI of the dataset
	 * @throws XMLStreamException
	 */
	private static void writeResource(XMLStreamWriter w, RepositoryConnection con, String cl, IRI uri) 
			throws XMLStreamException {
		w.writeStartElement(cl);
		w.writeAttribute("rdf:about", uri.toString());

		writeGeneric(w, con, uri);

		writeLiterals(w, con, uri, OWL.VERSIONINFO, "owl:versionInfo");
		writeLiterals(w, con, uri, DCAT.KEYWORD, "dcat:keyword");
		writeReferences(w, con, uri, DCTERMS.SUBJECT, "dct:subject");
		writeReferences(w, con, uri, DCAT.THEME, "dcat:theme");
		writeReferences(w, con, uri, DCAT.LANDING_PAGE, "dcat:landingPage", "foaf:Document", false);
		writeReferences(w, con, uri, FOAF.PAGE, "foaf:Page", "foaf:Document", false);
		writeReferences(w, con, uri, DCAT.ENDPOINT_URL, "dcat:endpointURL");
		writeReferences(w, con, uri, DCAT.SERVES_DATASET, "dcat:servesDataset");
		writeReferences(w, con, uri, FOAF.PRIMARY_TOPIC, "foaf:isPrimaryTopicOf", "dcat:CatalogRecord", false);

		//samples (geo-dcat-ap)
		try (RepositoryResult<Statement> res = con.getStatements(uri, ADMS_SAMPLE, null)) {
			while (res.hasNext()) {
				w.writeStartElement("adms:sample");
				writeDist(w, con, (IRI) res.next().getObject());
				w.writeEndElement();
			}
		}
		// full distributions
		try (RepositoryResult<Statement> res = con.getStatements(uri, DCAT.HAS_DISTRIBUTION, null)) {
			while (res.hasNext()) {
				w.writeStartElement("dcat:distribution");
				writeDist(w, con, (IRI) res.next().getObject());
				w.writeEndElement();
			}
		}

		writeContacts(w, con, uri, DCAT.CONTACT_POINT);

		writeReferences(w, con, uri, DCTERMS.SPATIAL, "dct:spatial", "dct:Location", true);
		writeReferences(w, con, uri, DCTERMS.ACCRUAL_PERIODICITY, "dct:accrualPeriodicity", "dct:Frequency", true);
		writeReferences(w, con, uri, DCTERMS.PROVENANCE, "dct:provenance", "dct:ProvenanceStatement", false);
		writeDates(w, con, uri);

		writeProvenances(w, con, uri, PROV.QUALIFIED_ATTRIBUTION);

		writeRole(w, con, uri, GEO_CUSTODIAN, "geodcatap:custodian");
		writeRole(w, con, uri, GEO_DISTRIBUTOR, "geodcatap:distributor");
		writeRole(w, con, uri, GEO_ORIGINATOR, "geodcatap:originator");
		writeRole(w, con, uri, GEO_PROCESSOR, "geodcatap:processor");
		
		w.writeEndElement();
	}

	/**
	 * Check uniqueness of dct:identifier
	 * 
	 * @param con
	 * @param iri
	 * @return 
	 */
	private static boolean isUnique(RepositoryConnection con, IRI iri) {
		try (RepositoryResult<Statement> res = con.getStatements(iri, DCTERMS.IDENTIFIER, null)) {
			while (res.hasNext()) {
				String id = res.next().getObject().stringValue();
				if (id.isBlank()) {
					LOG.warn("Empty dct:identifier for {}", iri);
				} else {
					if (!IDENTIFIERS.add(id)) {
						LOG.error("{} ID {} already present, skipping ...", iri, id);
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Write DCAT datasets
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @throws XMLStreamException
	 */
	private static void writeDatasets(XMLStreamWriter w, RepositoryConnection con)
		throws XMLStreamException {
		int nr = 0;
		int duplicates = 0;

		try (RepositoryResult<Statement> res = con.getStatements(null, RDF.TYPE, DCAT.DATASET)) {
			while (res.hasNext()) {
				IRI subj = (IRI) res.next().getSubject();
				if (!isUnique(con, subj)) {
					duplicates++;
					continue;
				}
				nr++;
				w.writeStartElement("dcat:dataset");
				writeResource(w, con, "dcat:Dataset", subj);
				w.writeEndElement();
			}
		}
		LOG.info("Wrote {} datasets, {} duplicates", nr, duplicates);
	}

	/**
	 * Write DCAT dataservice
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @throws XMLStreamException
	 */
	private static void writeDataservices(XMLStreamWriter w, RepositoryConnection con)
		throws XMLStreamException {
		int nr = 0;
		int duplicates = 0;

		try (RepositoryResult<Statement> res = con.getStatements(null, RDF.TYPE, DCAT.DATA_SERVICE)) {
			while (res.hasNext()) {
				IRI subj = (IRI) res.next().getSubject();
				if (!isUnique(con, subj)) {
					duplicates++;
					continue;
				}
				nr++;
				w.writeStartElement("dcat:service");
				writeResource(w, con, "dcat:DataService", subj);
				w.writeEndElement();
			}
		}
		LOG.info("Wrote {} services, {} duplicates", nr, duplicates);
	}

	/**
	 * Write multiple file formats
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the dataset
	 * @param pred RDF predicate
	 * @throws XMLStreamException
	 */
	private static void writeConcepts(XMLStreamWriter w, RepositoryConnection con, IRI pred, String classWrap) 
			throws XMLStreamException {
		
		Set<IRI> concepts;
	
		try (RepositoryResult<Statement> res = con.getStatements(null, pred, null)) {
			concepts = res.stream()
							.map(Statement::getObject)
							.filter(IRI.class::isInstance)
							.map(IRI.class::cast)
							.filter(i -> i.toString().startsWith("http"))
							.collect(Collectors.toSet());
		}

		if (concepts == null || concepts.isEmpty()) {
			return;
		}
		for (IRI concept: concepts) {
			w.writeStartElement(classWrap);
			w.writeAttribute("rdf:about", concept.toString());
			if(! classWrap.equals("skos:Concept")) {
				w.writeEmptyElement("rdf:type");
				w.writeAttribute("rdf:resource", SKOS.CONCEPT.stringValue());
			}
			writeLiterals(w, con, concept, SKOS.PREF_LABEL, "skos:prefLabel");
			writeReferences(w, con, concept, SKOS.IN_SCHEME, "skos:inScheme");
			w.writeEndElement();
		}
		LOG.info("Wrote {} {} concepts", concepts.size(), classWrap);
		
	}

	/**
	 * Write document (license, standard...) info
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @throws XMLStreamException
	 */
	private static void writeDocuments(XMLStreamWriter w, RepositoryConnection con, IRI pred, String classWrap) throws XMLStreamException {

		Set<IRI> documents;
	
		try (RepositoryResult<Statement> res = con.getStatements(null, pred, null)) {
			documents = res.stream()
							.map(Statement::getObject)
							.filter(IRI.class::isInstance)
							.map(IRI.class::cast)
							.filter(i -> i.toString().startsWith("http"))
							.collect(Collectors.toSet());		
		}

		if (documents == null || documents.isEmpty()) {
			return;
		}

		for (IRI document: documents) {
			w.writeStartElement(classWrap);
			w.writeAttribute("rdf:about", document.toString());
			writeGenericInfo(w, con, document);
			w.writeEndElement();
		}
		LOG.info("Wrote {} {} documents", documents.size(), classWrap);
	}

	/**
	 * Write location info
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @throws XMLStreamException
	 */
	private static void writeLocations(XMLStreamWriter w, RepositoryConnection con) throws XMLStreamException {
		int nr = 0;

		try (RepositoryResult<Statement> res = con.getStatements(null, RDF.TYPE, DCTERMS.LOCATION)) {
			while (res.hasNext()) {
				IRI iri = (IRI) res.next().getSubject();

				Value bbox = null;
				try(RepositoryResult<Statement> bboxes = con.getStatements(iri, DCAT.BBOX, null)) {
					while (bboxes.hasNext()) {
						Value val  = bboxes.next().getObject();
						if (val.stringValue().startsWith("POLYGON")) {
							bbox = val;
						}
					}
				}
				if (bbox != null) {
					nr++;
					w.writeStartElement("dct:Location");
					w.writeAttribute("rdf:about", iri.toString());
					writeLiteral(w, "dcat:bbox", bbox);
					w.writeEndElement();
				}
			}
		}
		LOG.info("Wrote {} locations", nr);
	}

	/**
	 * Write FOAF organization
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param iri iRI of the organization
	 * @throws XMLStreamException
	 */
	private static void writeAgent(XMLStreamWriter w, RepositoryConnection con, Resource iri) 
			throws XMLStreamException {
		w.writeStartElement("foaf:Agent");
		w.writeAttribute("rdf:about", iri.stringValue());
		if (con.hasStatement(iri, RDF.TYPE, FOAF.PERSON, false)) {
			writeType(w, con, iri, FOAF.PERSON);
			writeLiterals(w, con, iri, FOAF.NAME, "foaf:name");
			writeLiterals(w, con, iri, FOAF.GIVEN_NAME, "foaf:givenName");
			writeLiterals(w, con, iri, FOAF.FAMILY_NAME, "foaf:familyName");
			// mainly ORCID for researchers
			writeLiterals(w, con, iri, DCTERMS.IDENTIFIER, "dct:identifier");
			writeReferences(w, con, iri, ORG.MEMBER_OF, "org:memberOf");
		} else {
			writeType(w, con, iri, FOAF.ORGANIZATION);
			writeReferences(w, con, iri, DCTERMS.TYPE, "dct:type");
			writeLiterals(w, con, iri, FOAF.NAME, "foaf:name");
			writeReferences(w, con, iri, FOAF.HOMEPAGE, "foaf:homepage", "foaf:Document", false);
			writeReferences(w, con, iri, FOAF.WORKPLACE_HOMEPAGE, "foaf:workPlaceHomepage", "foaf:Document", false);
			writeReferences(w, con, iri, FOAF.MBOX, "foaf:mbox");
		}	
		w.writeEndElement();
	}

	/**
	 * Write FOAF organizations
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @throws XMLStreamException
	 */
	private static void writeAgents(XMLStreamWriter w, RepositoryConnection con)
		throws XMLStreamException {
		int nr = 0;

		try (RepositoryResult<Statement> res = con.getStatements(null, RDF.TYPE, FOAF.AGENT)) {
			while (res.hasNext()) {
				nr++;
				writeAgent(w, con, (IRI) res.next().getSubject());
			}
		}
		LOG.info("Wrote {} organizations", nr);
	}

	
	/**
	 * Write DCAT catalog to XML.
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 */
	private static void writeCatalog(XMLStreamWriter w, RepositoryConnection con)
		throws XMLStreamException {
		String cat = "http://data.gov.be/catalog";

		w.writeStartElement("rdf:RDF");
		writePrefixes(w);

		w.writeStartElement("dcat:Catalog");
		w.writeAttribute("dct:identifier", cat);
		w.writeAttribute("rdf:about", cat);

		IRI uri = con.getValueFactory().createIRI(cat);
		writeGeneric(w, con, uri);
		writeReferences(w, con, uri, FOAF.HOMEPAGE, "foaf:homepage");
		writeReferences(w, con, uri, DCTERMS.LICENSE, "dct:license");
		writeDatasets(w, con);
		writeDataservices(w, con);
		w.writeEndElement();

		writeAgents(w, con);

		writeLocations(w, con);
	
		writeDocuments(w, con, DCTERMS.PROVENANCE, "dct:ProvenanceStatement");
		writeDocuments(w, con, DCTERMS.LICENSE, "dct:LicenseDocument");
		writeDocuments(w, con, DCTERMS.RIGHTS, "dct:RightsStatement");
		writeDocuments(w, con, DCTERMS.CONFORMS_TO, "dct:Standard");

		writeConcepts(w, con, DCTERMS.ACCESS_RIGHTS, "dct:RightsStatement");
		writeConcepts(w, con, DCTERMS.ACCRUAL_PERIODICITY, "dct:Frequency");
		writeConcepts(w, con, DCTERMS.LANGUAGE, "dct:LinguisticSystem");
		writeConcepts(w, con, DCTERMS.FORMAT, "dct:MediaTypeOrExtent");
		writeConcepts(w, con, DCTERMS.TYPE, "skos:Concept");
		writeConcepts(w, con, DCAT.MEDIA_TYPE, "dct:MediaType");
		writeConcepts(w, con, DCAT.COMPRESS_FORMAT, "dct:MediaType");
		writeConcepts(w, con, DCAT.THEME, "skos:Concept");

		w.writeEndElement();
	}

	/**
	 * Return indenting XML serializer
	 *
	 * @return Saxon serializer
	 */
	private static Serializer getSerializer() {
		Configuration config = new Configuration();
		Processor processor = new Processor(config);
		Serializer s = processor.newSerializer();
		s.setOutputProperty(Property.METHOD, "xml");
		s.setOutputProperty(Property.ENCODING, "utf-8");
		s.setOutputProperty(Property.INDENT, "yes");
		s.setOutputProperty(Property.ESCAPE_URI_ATTRIBUTES, "yes");
		return s;
	}

	/**
	 * Check circular references (object same as subject)
	 * @param con 
	 */
	private static void checkCircular(RepositoryConnection con) {
		int circulars = 0;
		
		try(RepositoryResult<Statement> res = con.getStatements(null, RDF.TYPE, null)) {
			while(res.hasNext()) {
				Resource subj = res.next().getSubject();
				try(RepositoryResult<Statement> res2 = con.getStatements(subj, null, subj)) {
					while(res2.hasNext()) {
						Resource obj = res2.next().getPredicate();
						LOG.warn("Circular reference {} {}", subj, obj);
						circulars++;
					}
				}
			}
		}
		LOG.info("Detected {} circulars", circulars);
	}

	/**
	 * Main program
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		LOG.info("-- START --");
		if (args.length < 2) {
			LOG.error("No input or output file");
			System.exit(-1);
		}

		Optional<RDFFormat> fmtin = Rio.getParserFormatForFileName(args[0]);
		if (!fmtin.isPresent()) {
			LOG.error("No parser for input {}", args[0]);
			System.exit(-2);
		}

		Repository repo = new SailRepository(new MemoryStore());

		Serializer s = getSerializer();
		s.setOutputFile(new File(args[1]));

		try (RepositoryConnection con = repo.getConnection()) {
			ParserConfig cfg = new ParserConfig();
			cfg.set(BasicParserSettings.VERIFY_URI_SYNTAX, true);
			cfg.set(BasicParserSettings.VERIFY_RELATIVE_URIS, true);
			cfg.set(NTriplesParserSettings.FAIL_ON_INVALID_LINES, true);
	
			con.setParserConfig(cfg);
			con.add(new File(args[0]), BASE_URI, fmtin.get());
			
			checkCircular(con);

			XMLStreamWriter w = s.getXMLStreamWriter();

			w.writeStartDocument();
			writeCatalog(w, con);
			w.writeEndDocument();

			w.close();
		} catch (IOException | XMLStreamException | SaxonApiException ex) {
			LOG.error("Error converting", ex);
			System.exit(-1);
		} finally {
			repo.shutDown();
		}
		
		// verify once more

		RDFParser parser = Rio.createParser(RDFFormat.RDFXML);
		ParserConfig cfg = new ParserConfig();
		cfg.set(BasicParserSettings.VERIFY_URI_SYNTAX, true);
		cfg.set(BasicParserSettings.VERIFY_RELATIVE_URIS, true);
		cfg.set(XMLParserSettings.FAIL_ON_INVALID_QNAME, true);
		cfg.set(XMLParserSettings.FAIL_ON_MISMATCHED_TAGS, true);
		cfg.set(XMLParserSettings.FAIL_ON_NON_STANDARD_ATTRIBUTES, true);
		cfg.set(XMLParserSettings.FAIL_ON_SAX_NON_FATAL_ERRORS, true);
		parser.setParserConfig(cfg);
		parser.setValueFactory(new ValidatingValueFactory());

		try(Reader r = Files.newBufferedReader(Paths.get(args[1]))) {
			parser.parse(r, BASE_URI);
		} catch (Exception ex) {
			LOG.error("Error validating", ex);
			System.exit(-2);
		}
	}
}
