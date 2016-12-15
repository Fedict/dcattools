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

package be.fedict.dcat.classifier;

import be.fedict.dcat.helpers.Storage;

import com.google.common.collect.ListMultimap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dataset theme classifier
 * 
 * @author Bart.Hanssens
 */
public class Main {
	private final static Logger logger = LoggerFactory.getLogger(Main.class);

	private final static String PROP_PREFIX = "be.dcat.classifier";
	private final static Properties prop = new Properties();

	private final static Pattern WHITES = Pattern.compile("\\s");
	private final static Pattern ALPHA = Pattern.compile("\\w");
	
	private static Storage store = null;

	/**
     * Exit cleanly.
     * 
     * @param code return code 
     */
    private static void exit(int code) {
        if (store != null) {
            try {
                store.shutdown();
            } catch (RepositoryException ex) {
                logger.error("Error shutting down repository", ex);
            }
        }    
        logger.info("-- STOP --");
        System.exit(code);
    }
	
    /**
     * Read RDF file.
     */
    private static void readRDF() {
        String file = prop.getProperty(PROP_PREFIX + ".store");
        store = new Storage(new File(file));       
        try {
            store.startup();
        } catch (RepositoryException ex) {
            logger.error("Error starting repository", ex);
            exit(-3);
        }
        
        String rdfin = prop.getProperty(PROP_PREFIX + ".rdfin");
        try {
            store.read(new BufferedReader(new FileReader(rdfin)));
        } catch (IOException ex ) {
            logger.error("Could not read from rdf file {}", rdfin, ex);
            exit(-4);
        } catch (RepositoryException|RDFParseException ex) {
            logger.error("Repository error", ex);
            exit(-4);
        }    
    }
	
	/**
	 * Sanitize strings
	 * 
	 * @param str input string
	 * @return lowercase string without punctuation
	 */
	private static String sanitize(String str) {
		if (str == null || str.isEmpty()){
			return "";
		}
		String s = WHITES.matcher(str).replaceAll(" ");
		s = ALPHA.matcher(s).replaceAll(" ");
		return s.toLowerCase();
	}

	/**
	 * Sanitize a list of strings
	 * 
	 * @param str input string
	 * @return lowercase string without punctuation
	 */
	private static String sanitize(List<String> strs) {
		if (strs == null || strs.isEmpty()){
			return "";
		}
		String s = "";
		
		for(String str: strs) {
			String tmp = WHITES.matcher(str).replaceAll(" ");
			tmp = ALPHA.matcher(tmp).replaceAll(" ");
			s += tmp + " ";
		}
		return s.toLowerCase();
	}
	
	
	/**
	 * Write ARFF header file
	 * 
	 * @param w output writer
	 * @param langs languages
	 * @throws IOException 
	 */
	private static void writeARFFHeader(BufferedWriter w, String[] langs) throws IOException {
		w.append("@attribute id string");
		for (String lang: langs) {
			w.append("@attribute title").append(lang).append(" string");
			w.append("@attribute desc").append(lang).append(" string");
			w.append("@attribute keywords").append(lang).append(" string");
		}
		w.newLine();
		w.newLine();
		w.append("@data");
		w.newLine();
	}
	
	/**
	 * Write data line to ARFF file
	 * 
	 * @param w output writer
	 * @param langs languages
	 * @param uri IRI of the dataset
	 * @throws IOException 
	 */
	private static void writeARFFLine(BufferedWriter w, String[] langs, IRI uri) throws IOException {		
		Map<IRI, ListMultimap<String, String>> fields = store.queryProperties(uri);
		for (String lang : langs) {
			String title = sanitize(Storage.getOne(fields, DCTERMS.TITLE, lang));
			String desc = sanitize(Storage.getOne(fields, DCTERMS.DESCRIPTION, lang));
			String kw = sanitize(Storage.getMany(fields, DCAT.KEYWORD, lang));
			String themes = sanitize(Storage.getMany(fields, DCAT.THEME, ""));

			w.append(title).append(',')
					.append(desc).append(',')
					.append(kw);
		}
		w.newLine();
	}

	/**
	 * Write ARFF file for Meka/Weka
	 */
	private static void writeARFF() {
        String file = prop.getProperty(PROP_PREFIX + ".arff", "");
		if (file.isEmpty()) {
			logger.error("No output ARFF file defined");
			exit(-5);
		}
		
		String[] langs = prop.getProperty(PROP_PREFIX + ".languages", "").split(",");
		if (langs.length == 0 || langs[0].isEmpty()) {
			logger.error("No languages defined");
			exit(-6);
		}
		
		try (BufferedWriter w = Files.newBufferedWriter(Paths.get(file))) {
			writeARFFHeader(w, langs);
		
			List<IRI> uris = store.query(DCAT.DATASET);
			for(IRI uri: uris) {
				writeARFFLine(w, langs, uri);
			}
			w.close();
		} catch (IOException ex) {
			logger.error("Error writing to arff", ex);
			exit(-7);
		}
	}
    
		
	/**
     * Main program
     * 
     * @param args 
     */
    public static void main(String[] args)  {
        logger.info("-- START --");
		if (args.length == 0) {
            logger.error("No config file");
            exit(-1);
        }
        
        File config = new File(args[0]);
        try {
            prop.load(new FileInputStream(config));
        } catch (IOException ex) {
            logger.error("I/O Exception while reading {}", config, ex);
            exit(-2);
        }
		readRDF();
		
		writeARFF();
	}
}
