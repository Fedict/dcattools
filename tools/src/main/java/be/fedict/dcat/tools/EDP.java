/*
 * Copyright (c) 2016, Bart Hanssens <bart.hanssens@fedict.be>
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

import java.util.Optional;
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
import org.eclipse.rdf4j.model.vocabulary.VCARD4;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts to "Spanish" XML serialization, for the European Data Portal
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class EDP {

	private final static Logger logger = LoggerFactory.getLogger(EDP.class);

	private final static String PROP_PREFIX = "be.fedict.dcat.tools.edp";

	private final static String BELGIF_PREFIX = "http://org.belgif.be";
	private final static String ANYURI = "http://www.w3.org/2001/XMLSchema#anyURI";

	private final static SimpleValueFactory F = SimpleValueFactory.getInstance();
	private final static IRI STARTDATE = F.createIRI("http://schema.org/startDate");
	private final static IRI ENDDATE = F.createIRI("http://schema.org/endDate");

	/**
	 * Write XML namespace prefixes
	 *
	 * @param w writer
	 * @throws XMLStreamException
	 */
	private static void writePrefixes(XMLStreamWriter w) throws XMLStreamException {
		w.writeNamespace(DCAT.PREFIX, DCAT.NAMESPACE);
		w.writeNamespace("dct", DCTERMS.NAMESPACE);
		w.writeNamespace(FOAF.PREFIX, FOAF.NAMESPACE);
		w.writeNamespace(RDF.PREFIX, RDF.NAMESPACE);
		w.writeNamespace(RDFS.PREFIX, RDFS.NAMESPACE);
		w.writeNamespace("schema", "http://schema.org/");
		w.writeNamespace(VCARD4.PREFIX, VCARD4.NAMESPACE);
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
			String lang = ((Literal) val).getLanguage().orElse("");
			if (!lang.isEmpty()) {
				w.writeAttribute("xml:lang", lang);
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
	 * Write multiple format
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the dataset
	 * @param pred RDF predicate
	 * @throws XMLStreamException
	 */
	private static void writeFormats(XMLStreamWriter w, RepositoryConnection con,
		IRI uri, IRI pred) throws XMLStreamException {
		try (RepositoryResult<Statement> res = con.getStatements(uri, pred, null)) {
			if (!res.hasNext()) {
				return;
			}
			Value v = res.next().getObject();
			if (v instanceof IRI) {
				IRI fmt = (IRI) v;

				try (RepositoryResult<Statement> vl = con.getStatements(fmt, RDF.VALUE, null)) {
					if (vl.hasNext()) {
						w.writeStartElement("dcat:mediaType");
						Value val = vl.next().getObject();
						//w.writeAttribute("rdf:value", val.stringValue());
						w.writeCharacters(val.stringValue());
						w.writeEndElement();
					}
				}

				w.writeStartElement("dct:format");
				try (RepositoryResult<Statement> lbl = con.getStatements(fmt, DCTERMS.FORMAT, null)) {
					if (lbl.hasNext()) {
						Value val = lbl.next().getObject();
						w.writeAttribute("rdf:about", val.stringValue());
					} else {
						logger.error("No resource for format {}", fmt);
					}
				}
				w.writeEmptyElement("dct:IMT");
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
	 * Write multiple format
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the dataset
	 * @param pred RDF predicate
	 * @throws XMLStreamException
	 */
	private static void writeLicenses(XMLStreamWriter w, RepositoryConnection con,
		IRI uri, IRI pred) throws XMLStreamException {
		try (RepositoryResult<Statement> res = con.getStatements(uri, pred, null)) {
			if (res.hasNext()) {
				Value v = res.next().getObject();
				if (v instanceof IRI) {
					IRI license = (IRI) v;
					w.writeStartElement("dct:license");
					w.writeStartElement("dct:LicenseDocument");
					writeLiterals(w, con, license, DCTERMS.TITLE, "dct:title");
					w.writeEndElement();
					w.writeEndElement();
				} else {
					logger.error("Not a license IRI {}", v.stringValue());
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
					w.writeStartElement("vcard:Organization");
					writeLiterals(w, con, contact, VCARD4.FN, "vcard:fn");
					writeReferences(w, con, contact, VCARD4.HAS_EMAIL, "vcard:hasEmail");
					w.writeEndElement();
					w.writeEndElement();
				} else {
					logger.error("Not a contac IRI {}", v.stringValue());
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
	private static void writeReference(XMLStreamWriter w, String el, Value uri)
		throws XMLStreamException {
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
	private static void writeReferences(XMLStreamWriter w, RepositoryConnection con,
		Resource uri, IRI pred, String el) throws XMLStreamException {
		try (RepositoryResult<Statement> res = con.getStatements(uri, pred, null)) {
			while (res.hasNext()) {
				writeReference(w, el, res.next().getObject());
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
		writeReferences(w, con, uri, DCTERMS.LANGUAGE, "dct:language");
		writeLiterals(w, con, uri, DCTERMS.IDENTIFIER, "dct:identifier");
		writeLiterals(w, con, uri, DCTERMS.TITLE, "dct:title");
		writeLiterals(w, con, uri, DCTERMS.DESCRIPTION, "dct:description");
		writeLiterals(w, con, uri, DCTERMS.ISSUED, "dct:issued");
		writeLiterals(w, con, uri, DCTERMS.MODIFIED, "dct:modified");
		writeReferences(w, con, uri, DCTERMS.PUBLISHER, "dct:publisher");
		writeReferences(w, con, uri, DCTERMS.RIGHTS, "dct:rights");
		writeReferences(w, con, uri, DCTERMS.SPATIAL, "dct:spatial");
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

		//	writeReferences(w, con, uri, DCTERMS.FORMAT, "dct:format");
		writeFormats(w, con, uri, DCAT.MEDIA_TYPE);

		// write as anyURI string
		writeReferences(w, con, uri, DCAT.ACCESS_URL, "dcat:accessURL");
		writeReferences(w, con, uri, DCAT.DOWNLOAD_URL, "dcat:downloadURL");

		writeLicenses(w, con, uri, DCTERMS.LICENSE);

		w.writeEndElement();
	}

	/**
	 * Write DCAT dataset
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the dataset
	 * @throws XMLStreamException
	 */
	private static void writeDataset(XMLStreamWriter w, RepositoryConnection con,
		IRI uri) throws XMLStreamException {
		w.writeStartElement("dcat:Dataset");
		w.writeAttribute("rdf:about", uri.stringValue());

		writeGeneric(w, con, uri);

		writeLiterals(w, con, uri, DCAT.KEYWORD, "dcat:keyword");
		writeReferences(w, con, uri, DCAT.THEME, "dcat:theme");
		writeReferences(w, con, uri, DCAT.LANDING_PAGE, "dcat:landingPage");

		try (RepositoryResult<Statement> res = con.getStatements(uri, DCAT.HAS_DISTRIBUTION, null)) {
			while (res.hasNext()) {
				w.writeStartElement("dcat:distribution");
				writeDist(w, con, (IRI) res.next().getObject());
				w.writeEndElement();
			}
		}
		writeContacts(w, con, uri, DCAT.CONTACT_POINT);
		writeReferences(w, con, uri, DCTERMS.ACCRUAL_PERIODICITY, "dct:accrualPeriodicity");

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
				writeDataset(w, con, (IRI) res.next().getObject());
				w.writeEndElement();
			}
		}
		logger.info("Wrote {} dataset", nr);
	}

	/**
	 * Write FOAF organization
	 *
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the organization
	 * @throws XMLStreamException
	 */
	private static void writeOrg(XMLStreamWriter w, RepositoryConnection con,
		IRI uri) throws XMLStreamException {
		if (uri.stringValue().startsWith(BELGIF_PREFIX)) {
			w.writeStartElement("foaf:Organization");
			w.writeAttribute("rdf:about", uri.stringValue());

			writeLiterals(w, con, uri, FOAF.NAME, "foaf:name");
			writeReferences(w, con, uri, FOAF.HOMEPAGE, "foaf:homepage");
			writeReferences(w, con, uri, FOAF.MBOX, "foaf:mbox");

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
	private static void writeOrgs(XMLStreamWriter w, RepositoryConnection con)
		throws XMLStreamException {
		int nr = 0;

		try (RepositoryResult<Statement> res = con.getStatements(null, RDF.TYPE, FOAF.ORGANIZATION)) {
			while (res.hasNext()) {
				nr++;
				writeOrg(w, con, (IRI) res.next().getSubject());
			}
		}
		logger.info("Wrote {} organizations", nr);
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
		writeLicenses(w, con, uri, DCTERMS.LICENSE);
		writeDatasets(w, con);

		w.writeEndElement();

		writeOrgs(w, con);

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
		repo.initialize();

		Serializer s = getSerializer();
		s.setOutputFile(new File(args[1]));

		try (RepositoryConnection con = repo.getConnection()) {
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
