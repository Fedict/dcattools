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
 * Generate ARFF for machine learning tools.
 * 
 * @author Bart.Hanssens
 */
public class ARFF {
	private final static Logger logger = LoggerFactory.getLogger(ARFF.class);

	private final static String PROP_PREFIX = "be.fedict.dcat.tools.arff";
	private final static Properties prop = new Properties();

	private final static Pattern WHITES = Pattern.compile("\\s+");
	private final static Pattern ALPHA = Pattern.compile("\\W+");
	
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
        String file = prop.getProperty(PROP_PREFIX + ".store", "");
        store = new Storage(new File(file));
        try {
            store.startup();
        } catch (RepositoryException ex) {
            logger.error("Error starting repository", ex);
            exit(-3);
        }
        
        String rdfin = prop.getProperty(PROP_PREFIX + ".rdfin", "");
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
	 * Sanitize list of strings
	 * 
	 * @param strs array of input string
	 * @return lowercase string without punctuation
	 */
	private static String sanitize(List<String> strs) {
		if (strs == null || strs.isEmpty()){
			return "";
		}
		
		String s = "";
		for (String str: strs) {
			String tmp = WHITES.matcher(str).replaceAll(" ");
			s += ALPHA.matcher(tmp).replaceAll(" ");
		}
		return s.toLowerCase();
	}
	
	/**
	 * Turn a list of themes into boolean strings
	 * 
	 * @param dataset list of dataset themes
	 * @param all list of all possible themes
	 * @return comma-separated string of booleans
	 */
	private static String[] bitThemes(List<String> dataset, String[] all) {
		dataset.removeIf(str -> !str.startsWith("http://publications.europa.eu"));
		dataset.replaceAll(str -> str.substring(str.lastIndexOf("/") + 1, str.lastIndexOf("/") + 5));
		
		String[] bools = new String[all.length];
		int i = 0;
		for (String theme: all) {
			bools[i++] = dataset.contains(theme) ? "1" : "0";
		}
		
		return bools;
	}
	
	
	/**
	 * Write ARFF header file
	 * 
	 * @param w output writer
	 * @param langs languages
	 * @param themes DCAT themes
	 * @throws IOException 
	 */
	private static void writeARFFHeader(BufferedWriter w, String[] langs, String[] themes) 
													throws IOException {
		w.append("@relation 'DCAT datasets:")
				.append(" -C -").append(String.valueOf(themes.length)).append("'");
		w.newLine();
		w.newLine();
		
		w.append("@attribute id string");
		w.newLine();
		for (String lang: langs) {
			w.append("@attribute title").append(lang).append(" string");
			w.newLine();
			w.append("@attribute desc").append(lang).append(" string");
			w.newLine();
			w.append("@attribute keywords").append(lang).append(" string");
			w.newLine();
		}
		for (String theme: themes) {
			w.append("@attribute ").append(theme).append(" {0,1}");
			w.newLine();
		}

		w.newLine();
		w.append("@data");
		w.newLine();
		w.newLine();
	}
	
	/**
	 * Write data line to ARFF file
	 * 
	 * @param w output writer
	 * @param langs languages
	 * @param themes DCAT themes
	 * @param uri IRI of the dataset
	 * @throws IOException 
	 */
	private static void writeARFFLine(BufferedWriter w, String[] langs, String[] themes, IRI uri) 
															throws IOException {		
		Map<IRI, ListMultimap<String, String>> fields = store.queryProperties(uri);
		
		w.append(uri.stringValue()).append(',');
		
		for (String lang : langs) {
			String title = sanitize(Storage.getOne(fields, DCTERMS.TITLE, lang));
			String desc = sanitize(Storage.getOne(fields, DCTERMS.DESCRIPTION, lang));
			String kw = sanitize(Storage.getMany(fields, DCAT.KEYWORD, lang));

			w.append('\'').append(title).append('\'').append(',')
					.append('\'').append(desc).append('\'').append(',')
					.append('\'').append(kw).append('\'').append(',');
		}
		
		String[] themebits = bitThemes(Storage.getMany(fields, DCAT.THEME, ""), themes);
		int i = 1;
		for (String themebit: themebits) {
			w.append(themebit);
			if (i++ < themebits.length) {
				w.append(',');
			}
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

		String[] themes = prop.getProperty(PROP_PREFIX + ".themes", "").split(",");
		if (themes.length == 0 || themes[0].isEmpty()) {
			logger.error("No themes defined");
			exit(-7);
		}
		
		try (BufferedWriter w = Files.newBufferedWriter(Paths.get(file))) {
			writeARFFHeader(w, langs, themes);
		
			List<IRI> uris = store.query(DCAT.DATASET);
			for(IRI uri: uris) {
				writeARFFLine(w, langs, themes, uri);
			}
			w.close();
		} catch (IOException ex) {
			logger.error("Error writing to arff", ex);
			exit(-8);
		}
	}
    
		
	/**
     * ARFF program
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