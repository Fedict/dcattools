/*
 * Copyright (c) 2015, Bart Hanssens <bart.hanssens@fedict.be>
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
package be.fedict.dcat.enhancers;

import be.fedict.dcat.helpers.Storage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class. Enhance DCAT files by running various mappers etc.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);
    
    private final static Properties prop = new Properties();
    private static Storage store = null;
    
    
    /**
     * Exit cleanly
     * 
     * @param code return code 
     */
    protected static void exit(int code) {
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
     * Run enhancer(s) and write to output file.
     */
    private static void enhance() {
        int i = 1;
        while(prop.containsKey(Enhancer.PROP_PREFIX + "." + Integer.toString(i) + ".classname")) {
            enhancer(Enhancer.PROP_PREFIX + "." + Integer.toString(i));
            i++;
        }
        
        String rdfout = prop.getProperty(Enhancer.PROP_PREFIX + ".rdfout");
        try {
            store.write(new BufferedWriter(new FileWriter(rdfout)));
        } catch (IOException|RepositoryException ex ) {
            logger.error("Could not write to rdf file {}", rdfout, ex);
            exit(-5);
        }
    }
    
    /**
     * Read RDF file.
     */
    private static void readRDF() {
            String file = prop.getProperty(Enhancer.PROP_PREFIX + ".store");
        store = new Storage(new File(file));       
        try {
            store.startup();
        } catch (RepositoryException ex) {
            logger.error("Error starting repository", ex);
            exit(-3);
        }
        
        String rdfin = prop.getProperty(Enhancer.PROP_PREFIX + ".rdfin");
        try {
            store.read(new BufferedReader(new FileReader(rdfin)));
        } catch (IOException|RepositoryException|RDFParseException ex ) {
            logger.error("Could not read from rdf file {}", rdfin, ex);
            exit(-4);
        }
    }
    
    
    /**
     * Load an enhancer and enhance the RDF triples.
     * 
     * @param prefix properties prefix for additional configuration
     */
    private static void enhancer(String prefix) {
        String name = prop.getProperty(prefix + ".classname");
        
        try {
            Class<? extends Enhancer> c = Class.forName(name).asSubclass(Enhancer.class);
            Enhancer e = c.getConstructor(Storage.class).newInstance(store);
            e.setProperties(prop, prefix);
            e.enhance();
        } catch (ClassNotFoundException|InstantiationException|NoSuchMethodException|
                            IllegalAccessException|InvocationTargetException ex) {
            logger.error("Enhancer class {} could not be loaded", name, ex);
        }
    }
    
    /**
     * Main program
     * 
     * @param args 
     */
    public static void main(String[] args) {
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
        
        enhance();
        
        exit(0);
    }
}
