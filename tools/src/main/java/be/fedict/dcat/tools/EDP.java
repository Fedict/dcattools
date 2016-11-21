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
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Optional;
import java.util.logging.Level;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.rdfxml.util.RDFXMLPrettyWriterFactory;
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


	/**
	 * Write XML namespace prefixes
	 * 
	 * @param w writer
	 * @throws XMLStreamException
	 */			
	private static void writePrefixes(XMLStreamWriter w) throws XMLStreamException {
		w.setPrefix(DCAT.PREFIX, DCAT.NAMESPACE);
		w.setPrefix(DCTERMS.PREFIX, DCTERMS.NAMESPACE);
		w.setPrefix(FOAF.PREFIX, FOAF.NAMESPACE);
		w.setPrefix(RDF.PREFIX, RDF.NAMESPACE);
		w.setPrefix(RDFS.PREFIX, RDFS.NAMESPACE);
	}

	private static void writeReference(XMLStreamWriter w, String el, Value val)
			throws XMLStreamException {
		if (val instanceof IRI) {
			w.writeEmptyElement(el);
			w.writeAttribute("rdf:resource", ((IRI) val).stringValue());
		}
	}
	
	/**
	 * Write literal to RDF
	 * 
	 * @param w
	 * @param el
	 * @param val
	 * @throws XMLStreamException 
	 */
	private static void writeLiteral(XMLStreamWriter w, String el, Value val) 
			throws XMLStreamException {
		if (val instanceof Literal) {
			w.writeStartElement(el);
			w.writeAttribute("xml:lang", ((Literal) val).getLanguage().orElse(""));
			w.writeCharacters(val.stringValue());
			w.writeEndElement();
		}
	}
	
	private static void writeGeneric(XMLStreamWriter w, RepositoryConnection con,
			Resource uri) throws XMLStreamException {
		try (RepositoryResult<Statement> res = con.getStatements(uri, DCTERMS.LANGUAGE, null)) {
			while (res.hasNext()) {
				writeReference(w, "dcterms:language", res.next().getObject());
			}
		}
		try (RepositoryResult<Statement> res = con.getStatements(uri, DCTERMS.TITLE, null)) {
			while (res.hasNext()) {
				writeLiteral(w, "dcterms:title", res.next().getObject());
			}
		}
		try (RepositoryResult<Statement> res = con.getStatements(uri, DCTERMS.SUBJECT, null)) {
			while (res.hasNext()) {
				writeLiteral(w, "dcterms:subject", res.next().getObject());
			}
		}
		try (RepositoryResult<Statement> res = con.getStatements(uri, DCTERMS.ISSUED, null)) {
			while (res.hasNext()) {
				writeLiteral(w, "dcterms:issued", res.next().getObject());
			}
		}		
		try (RepositoryResult<Statement> res = con.getStatements(uri, DCTERMS.MODIFIED, null)) {
			while (res.hasNext()) {
				writeLiteral(w, "dcterms:modified", res.next().getObject());
			}
		}
		try (RepositoryResult<Statement> res = con.getStatements(uri, DCTERMS.RIGHTS, null)) {
			while (res.hasNext()) {
				writeReference(w, "dcterms:rights", res.next().getObject());
			}
		}
		try (RepositoryResult<Statement> res = con.getStatements(uri, DCTERMS.LICENSE, null)) {
			while (res.hasNext()) {
				writeReference(w, "dcterms:license", res.next().getObject());
			}
		}
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
			Resource uri) throws XMLStreamException {
		w.writeStartElement("dcat:Distribution");	
		w.writeAttribute("rdf:about", uri.stringValue());

		writeGeneric(w, con, uri);
		
		try (RepositoryResult<Statement> res = con.getStatements(uri, null, null)) {
			while (res.hasNext()) {
				w.writeEndElement();
			}
		}

		w.writeEndElement();
	}
	
	/**
	 * Write dcat dataset
	 * 
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @param uri URI of the dataset
	 * @throws XMLStreamException 
	 */
	private static void writeDataset(XMLStreamWriter w, RepositoryConnection con,
			Resource uri) throws XMLStreamException {
		w.writeStartElement("dcat:Dataset");	
		w.writeAttribute("rdf:about", uri.stringValue());

		writeGeneric(w, con, uri);
		
		try (RepositoryResult<Statement> res = con.getStatements(uri, DCAT.HAS_DISTRIBUTION, null)) {
			while (res.hasNext()) {
				w.writeStartElement("dcat:distribution");
				writeDist(w, con, (Resource) res.next().getObject());
				w.writeEndElement();
			}
		}
		
		w.writeEndElement();
	}	

	/**
	 * Write dcat datasets
	 * 
	 * @param w XML writer
	 * @param con RDF triple store connection
	 * @throws XMLStreamException 
	 */
	private static void writeDatasets(XMLStreamWriter w, RepositoryConnection con) 
			throws XMLStreamException {

		try (RepositoryResult<Statement> res = con.getStatements(null, DCAT.HAS_DATASET, null)) {
			while (res.hasNext()) {
				w.writeStartElement("dcat:dataset");
				writeDataset(w, con, (Resource) res.next().getObject());
				w.writeEndElement();
			}
		}
	}
	
	/**
	 * 
	 * @param w 
	 */
	private static void writeCatalog(XMLStreamWriter w, RepositoryConnection con) 
			throws XMLStreamException {
		w.writeStartElement("rdf:RDF");
		w.writeStartElement("dcat:Catalog");
		w.writeAttribute("dcterms:identifier", "http://data.gov.be/catalog#id");
		w.writeAttribute("rdf:about", "http://data.gov.be/catalog#id");
	
		writeDatasets(w, con);
		
	//	writeOrgs(w, con);
		
		w.writeEndElement();
		w.writeEndElement();
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
        if(!fmtin.isPresent()) {
            logger.error("No parser for input {}", args[0]);
            System.exit(-2);
        }
        
		XMLOutputFactory xfac = XMLOutputFactory.newInstance();
        
        int code = 0;
        Repository repo = new SailRepository(new MemoryStore());
		repo.initialize();
        
        try (RepositoryConnection con = repo.getConnection();
				FileOutputStream fout = new FileOutputStream(args[1])) {
			
            con.add(new File(args[0]), "http://data.gov.be", fmtin.get());
			XMLStreamWriter w = xfac.createXMLStreamWriter(fout);
			
			w.writeStartDocument();
			writePrefixes(w);
			writeCatalog(w, con);
			w.writeEndDocument();
	
			w.close();		
		} catch (IOException|XMLStreamException ex) {
            logger.error("Error converting", ex);
            code = -1;
        }
		
        repo.shutDown();
        
        System.exit(code);
    }    
}
