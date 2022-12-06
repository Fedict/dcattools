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
package be.fedict.dcat.tools;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;

import java.util.Set;
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
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.NTriplesParserSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts to a "nice" XML serialization, for the European Data Portal and other users.
 *
 * @author Bart Hanssens
 */
public class EDP {

	private final static Logger logger = LoggerFactory.getLogger(EDP.class);

	private final static String BELGIF_PREFIX = "http://org.belgif.be";
	private final static String ANYURI = "http://www.w3.org/2001/XMLSchema#anyURI";

	private final static SimpleValueFactory F = SimpleValueFactory.getInstance();
	private final static IRI STARTDATE = F.createIRI("http://schema.org/startDate");
	private final static IRI ENDDATE = F.createIRI("http://schema.org/endDate");
	private final static IRI ADMS_IDENTIFIER = F.createIRI("http://www.w3.org/ns/adms#identifier");

	private final static Set<IRI> CONCEPTS = new HashSet<>();

	/**
	 * Write XML namespace prefixes
	 *
	 * @param w writer
	 * @throws XMLStreamException
	 */
	private static void writePrefixes(XMLStreamWriter w) throws XMLStreamException {
		w.writeNamespace("adms", "http://www.w3.org/ns/adms#");
		w.writeNamespace(DCAT.PREFIX, DCAT.NAMESPACE);
		w.writeNamespace("dct", DCTERMS.NAMESPACE);
		w.writeNamespace(FOAF.PREFIX, FOAF.NAMESPACE);
		w.writeNamespace(RDF.PREFIX, RDF.NAMESPACE);
		w.writeNamespace(RDFS.PREFIX, RDFS.NAMESPACE);
		w.writeNamespace("schema", "http://schema.org/");
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
		IRI uri, IRI pred, String el) throws XMLStreamException {
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
				if (v instanceof IRI) {
					IRI date = (IRI) v;
					w.writeStartElement("dct:temporal");
					w.writeStartElement("dct:PeriodOfTime");
					writeLiterals(w, con, date, STARTDATE, "schema:startDate");
					writeLiterals(w, con, date, ENDDATE, "schema:endDate");
					w.writeEndElement();
					w.writeEndElement();
				} else {
					logger.error("Not a date IRI {}", v.stringValue());
				}
			}
		}
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
	private static void writeFormats(XMLStreamWriter w, RepositoryConnection con,
		IRI uri, IRI pred, String classWrap) throws XMLStreamException {
		try (RepositoryResult<Statement> res = con.getStatements(uri, pred, null)) {
			if (!res.hasNext()) {
				return;
			}
			Value v = res.next().getObject();
			if (v instanceof IRI) {
				IRI fmt = (IRI) v;
				addSkosConcept(fmt);

				w.writeStartElement(classWrap);
				w.writeEmptyElement("dct:MediaType");
				w.writeAttribute("rdf:about", fmt.toString());

				try (RepositoryResult<Statement> lbl = con.getStatements(fmt, RDFS.LABEL, null)) {
					if (lbl.hasNext()) {
						Value val = lbl.next().getObject();
						w.writeAttribute("rdfs:label", val.stringValue().toUpperCase());
					} else {
						logger.error("No label for format {}", fmt);
					}
				}

				w.writeEndElement();
			} else {
				logger.error("Not a format IRI {}", v.stringValue());
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
					writeReferences(w, con, contact, VCARD4.HAS_EMAIL, "vcard:hasEmail");
					writeReferences(w, con, contact, VCARD4.HAS_TELEPHONE, "vcard:hasTelephone");
					w.writeEndElement();
					w.writeEndElement();
				} else {
					logger.error("Not a contact IRI {}", v.stringValue());
				}
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
		if (uri instanceof IRI) {
			w.writeEmptyElement(el);
			w.writeAttribute("rdf:resource", ((IRI) uri).stringValue());
		} else {
			logger.error("Not a reference IRI {}", uri.stringValue());
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
					if( !refUri.toString().contains(".well-known")) {
						w.writeEmptyElement(classWrap);
						if (concept) {
							addSkosConcept(iri);
						}
						w.writeAttribute("rdf:about", iri.stringValue());
					} else {
						w.writeStartElement(classWrap);
						writeGenericInfo(w, con, iri);
						w.writeEndElement();
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
	 * Write generic metadata
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the dataset
	 * @throws XMLStreamException
	 */
	private static void writeGeneric(XMLStreamWriter w, RepositoryConnection con,
		IRI uri) throws XMLStreamException {
		writeReferences(w, con, uri, DCTERMS.LANGUAGE, "dct:language", "dct:LinguisticSystem", true);
		writeLiterals(w, con, uri, DCTERMS.IDENTIFIER, "dct:identifier");
		writeLiterals(w, con, uri, DCTERMS.TITLE, "dct:title");
		writeLiterals(w, con, uri, DCTERMS.DESCRIPTION, "dct:description");
		writeLiterals(w, con, uri, DCTERMS.ISSUED, "dct:issued");
		writeLiterals(w, con, uri, DCTERMS.MODIFIED, "dct:modified");
		writeReferences(w, con, uri, ADMS_IDENTIFIER, "adms:identifier", "adms:Identifier", false);
		writeReferences(w, con, uri, DCTERMS.PUBLISHER, "dct:publisher", "foaf:Agent", false);
		writeReferences(w, con, uri, DCTERMS.CONFORMS_TO, "dct:conformsTo", "dct:Standard", false);
		writeReferences(w, con, uri, DCTERMS.ACCESS_RIGHTS, "dct:accessRights", "dct:RightsStatement", false);
		writeReferences(w, con, uri, DCTERMS.RIGHTS, "dct:rights", "dct:RightsStatement", false);
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

		writeReferences(w, con, uri, DCAT.MEDIA_TYPE, "dcat:mediaType", "dct:MediaType", true);
		writeFormats(w, con, uri, DCTERMS.FORMAT, "dct:format");
		writeFormats(w, con, uri, DCAT.COMPRESS_FORMAT, "dcat:compressFormat");
		writeFormats(w, con, uri, DCAT.PACKAGE_FORMAT, "dcat:packageFormat");

		// write as anyURI string
		writeReferences(w, con, uri, DCAT.ACCESS_URL, "dcat:accessURL");
		writeReferences(w, con, uri, DCAT.ACCESS_SERVICE, "dcat:accessService");
		writeReferences(w, con, uri, DCAT.DOWNLOAD_URL, "dcat:downloadURL");

		writeReferences(w, con, uri, DCTERMS.LICENSE, "dct:license");

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
		w.writeAttribute("rdf:about", uri.stringValue());

		writeGeneric(w, con, uri);

		writeLiterals(w, con, uri, DCAT.KEYWORD, "dcat:keyword");
		writeReferences(w, con, uri, DCAT.THEME, "dcat:theme", "skos:Concept", false);
		writeReferences(w, con, uri, DCAT.LANDING_PAGE, "dcat:landingPage", "foaf:Document", false);
		writeReferences(w, con, uri, DCAT.ENDPOINT_URL, "dcat:endpointURL");
		writeReferences(w, con, uri, DCAT.SERVES_DATASET, "dcat:servesDataset");

		try (RepositoryResult<Statement> res = con.getStatements(uri, DCAT.HAS_DISTRIBUTION, null)) {
			while (res.hasNext()) {
				w.writeStartElement("dcat:distribution");
				writeDist(w, con, (IRI) res.next().getObject());
				w.writeEndElement();
			}
		}
		writeContacts(w, con, uri, DCAT.CONTACT_POINT);

		writeReferences(w, con, uri, DCTERMS.SPATIAL, "dct:spatial", "dct:Location", true);
		writeLiterals(w, con, uri, DCAT.SPATIAL_RESOLUTION_IN_METERS, "dcat:spatialResolutionInMeters");
		writeReferences(w, con, uri, DCTERMS.ACCRUAL_PERIODICITY, "dct:accrualPeriodicity", "dct:Frequency", true);
		writeLiterals(w, con, uri, DCAT.TEMPORAL_RESOLUTION, "dcat:temporalResolution");
		writeReferences(w, con, uri, DCTERMS.PROVENANCE, "dct:provenance", "dct:ProvenanceStatement", false);
		writeDates(w, con, uri);

		w.writeEndElement();
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

		try (RepositoryResult<Statement> res = con.getStatements(null, DCAT.HAS_DATASET, null)) {
			while (res.hasNext()) {
				nr++;
				w.writeStartElement("dcat:dataset");
				writeResource(w, con, "dcat:Dataset", (IRI) res.next().getObject());
				w.writeEndElement();
			}
		}
		logger.info("Wrote {} datasets", nr);
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

		try (RepositoryResult<Statement> res = con.getStatements(null, DCAT.HAS_SERVICE, null)) {
			while (res.hasNext()) {
				nr++;
				w.writeStartElement("dcat:service");
				writeResource(w, con, "dcat:DataService", (IRI) res.next().getObject());
				w.writeEndElement();
			}
		}
		logger.info("Wrote {} services", nr);
	}
	
	/**
	 * Write document (license, standard...) info
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @throws XMLStreamException
	 */
	private static void writeDocuments(XMLStreamWriter w, RepositoryConnection con, IRI obj, String classWrap) throws XMLStreamException {
		int nr = 0;

		try (RepositoryResult<Statement> res = con.getStatements(null, RDF.TYPE, obj)) {
			while (res.hasNext()) {
				IRI iri = (IRI) res.next().getSubject();
				if (! iri.stringValue().contains(".well-known")) {
					nr++;
					w.writeStartElement(classWrap);
					w.writeAttribute("rdf:about", iri.toString());
					writeGenericInfo(w, con, iri);
					w.writeEndElement();
				}
			}
		}
		logger.info("Wrote {} docs", nr);
	}

	/**
	 * Write document (license, standard...) info
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
				RepositoryResult<Statement> bboxes = con.getStatements(iri, DCAT.BBOX, null);
				while (bboxes.hasNext()) {
					Value val  = bboxes.next().getObject();
					if (val.stringValue().startsWith("POLYGON")) {
						bbox = val;
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
		logger.info("Wrote {} locations", nr);
	}

	/**
	 * Write FOAF organization
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param iri iRI of the organization
	 * @throws XMLStreamException
	 */
	private static void writeOrganization(XMLStreamWriter w, RepositoryConnection con, IRI iri) 
			throws XMLStreamException {
		if (iri.stringValue().startsWith(BELGIF_PREFIX)) {
			addSkosConcept(iri);
			w.writeStartElement("foaf:Organization");
			w.writeAttribute("rdf:about", iri.stringValue());

			writeLiterals(w, con, iri, FOAF.NAME, "foaf:name");
			writeReferences(w, con, iri, FOAF.HOMEPAGE, "foaf:homepage", "foaf:Document", false);
			
			writeReferences(w, con, iri, FOAF.MBOX, "foaf:mbox");

			w.writeEndElement();
		}
	}

	/**
	 * Write FOAF organizations
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @throws XMLStreamException
	 */
	private static void writeOrganizations(XMLStreamWriter w, RepositoryConnection con)
		throws XMLStreamException {
		int nr = 0;

		try (RepositoryResult<Statement> res = con.getStatements(null, RDF.TYPE, FOAF.ORGANIZATION)) {
			while (res.hasNext()) {
				nr++;
				writeOrganization(w, con, (IRI) res.next().getSubject());
			}
		}
		logger.info("Wrote {} organizations", nr);
	}

	/**
	 * Write SKOS concepts
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @throws XMLStreamException
	 */
	private static void writeConcepts(XMLStreamWriter w)
		throws XMLStreamException {
		int nr = 0;

		Set<String> schemes = new HashSet<>(); 
		for (IRI iri: CONCEPTS) {
			w.writeStartElement("skos:Concept");
			String concept = iri.toString();
			w.writeAttribute("rdf:about", concept);
			
			w.writeEmptyElement("skos:inScheme");
			String scheme = concept.contains("geonames") 
				? "https://sws.geonames.org"
				: (concept.contains("belgif") 
					? "https://org.belgif.be/id/CbeRegisteredEntity"
					: concept.substring(0, concept.lastIndexOf("/")));
			schemes.add(scheme);
			w.writeAttribute("rdf:resource", scheme);

			w.writeEndElement();
			nr++;
		}
		for (String scheme: schemes) {
			w.writeEmptyElement("skos:ConceptScheme");	
			w.writeAttribute("rdf:about", scheme);
		}
		logger.info("Wrote {} concepts", nr);
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

		writeDocuments(w, con, DCTERMS.LICENSE_DOCUMENT, "dct:LicenseDocument");
		writeDocuments(w, con, DCTERMS.RIGHTS_STATEMENT, "dct:RightsStatement");
		writeDocuments(w, con, DCTERMS.STANDARD, "dct:Standard");
		
		writeOrganizations(w, con);
		writeConcepts(w);
		writeLocations(w, con);

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
		return s;
	}

	/**
	 * Main program
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		logger.info("-- START --");
		if (args.length < 2) {
			logger.error("No input or output file");
			System.exit(-1);
		}

		Optional<RDFFormat> fmtin = Rio.getParserFormatForFileName(args[0]);
		if (!fmtin.isPresent()) {
			logger.error("No parser for input {}", args[0]);
			System.exit(-2);
		}

		Repository repo = new SailRepository(new MemoryStore());

		Serializer s = getSerializer();
		s.setOutputFile(new File(args[1]));

		try (RepositoryConnection con = repo.getConnection()) {
			con.getParserConfig().set(NTriplesParserSettings.FAIL_ON_INVALID_LINES, false);
			con.add(new File(args[0]), "http://data.gov.be", fmtin.get());
			XMLStreamWriter w = s.getXMLStreamWriter();


			w.writeStartDocument();
			writeCatalog(w, con);
			w.writeEndDocument();

			w.close();
		} catch (IOException | XMLStreamException | SaxonApiException ex) {
			logger.error("Error converting", ex);
			System.exit(-1);
		} finally {
			repo.shutDown();
		}
	}
}
