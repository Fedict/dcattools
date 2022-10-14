/*
 * Copyright (c) 2022, FPS BOSA DG DT
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
package be.gov.data.drupal;

import be.gov.data.drupal.dao.Term;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart.Hanssens
 */
public class Main {
	private final static Logger LOG = LoggerFactory.getLogger(Main.class);
	private final static CommandLineParser parser = new DefaultParser();
	private final static Options options = new Options();
	
	static {
		options.addOption("s", true, "SKOS input file");
		options.addOption("h", true, "Drupal 9 website");	
		options.addOption("u", true, "user name");		
		options.addOption("p", true, "password");
		options.addOption("t", true, "taxonomy");
	}


	/**
	 * Main program
	 * 
	 * @param args
	 * @throws ParseException 
	 * @throws IOException 
	 */
    public static void main(String[] args) throws ParseException, IOException {
		CommandLine line = parser.parse(options, args);
		String file = line.getOptionValue("s");
		String host = line.getOptionValue("h");
		String user= line.getOptionValue("u");
		String pass = line.getOptionValue("p");
		String taxo = line.getOptionValue("t");
			
		TaxonomyLoader loader = new TaxonomyLoader(host, user, pass);
		List<Term> terms = loader.parse(new File(file));
		for (Term t: terms) {
			LOG.info("Loading term {}", t.subject());
			loader.postTerm(taxo, t);
		}
	}
}
