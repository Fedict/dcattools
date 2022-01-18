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
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Optional;

import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.rdfxml.util.RDFXMLPrettyWriterFactory;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serialization format converter (e.g. N-Triples to RDF/XML)
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class Converter {
    private final static Logger logger = LoggerFactory.getLogger(Converter.class);
	
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
        
        Optional<RDFFormat> fmtout = Rio.getWriterFormatForFileName(args[1]);
        if(!fmtout.isPresent()) {
            logger.error("No parser for output {}", args[1]);
            System.exit(-3);            
        }
        
        int code = 0;
        Repository repo = new SailRepository(new MemoryStore());
		repo.initialize();
        
        try (RepositoryConnection con = repo.getConnection()) {
            con.add(new File(args[0]), "http://data.gov.be", fmtin.get());
			// Various namespace prefixes
            con.setNamespace(DCAT.PREFIX, DCAT.NAMESPACE);
			con.setNamespace(DCTERMS.PREFIX, DCTERMS.NAMESPACE);
			con.setNamespace(FOAF.PREFIX, FOAF.NAMESPACE);
			con.setNamespace("vcard", "http://www.w3.org/2006/vcard/ns#");
			con.setNamespace("locn", "http://www.w3.org/ns/locn#");
		
			FileOutputStream fout = new FileOutputStream(new File(args[1]));
			
			RDFWriter writer;
			if (fmtout.get().equals(RDFFormat.RDFXML)) {
				writer = new RDFXMLPrettyWriterFactory().getWriter(fout);
			} else {
				writer = Rio.createWriter(fmtout.get(), fout);
			}
		
			logger.info("Using writer {}", writer.getClass().getCanonicalName());
			
            con.export(writer);
        } catch (IOException ex) {
            logger.error("Error converting", ex);
            code = -1;
        }
        
        repo.shutDown();
        
        System.exit(code);
    }    
}
