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

import be.fedict.dcat.helpers.Fetcher;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple link checker.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class LinkChecker {
    private final static Logger logger = LoggerFactory.getLogger(LinkChecker.class);
    
    private final static String PROP_PREFIX = "be.fedict.dcat.tools.linkchecker";
    
    private final static Properties prop = new Properties();
    private final static Fetcher fetcher = new Fetcher();
    
    /**
     * Exit cleanly
     * 
     * @param code return code 
     */
    private static void exit(int code) {
        logger.info("-- STOP --");
        System.exit(code);
    }
    
    /**
     * Get property from config file.
     * 
     * @param name
     * @return property value
     */
    private static String getProperty(String name) {
        String p = PROP_PREFIX + "." + name;

        String value = prop.getProperty(p, "");
        if (value.isEmpty()) {
            logger.error("No property {}", p);
        }
        return value;
    }
    
    /**
     * Check URL after waiting a short period of time.
     * 
     * @param url string
     * @return HTTP Status code or -1
     */
    private static int checkURL(String url) {
        int code = -1;
        fetcher.sleep();
        
        logger.debug("Checking {}", url);
        try {
            // Catch URI exception early, otherwise Fluent will crash
            URI u = new URI(url);
            code = fetcher.makeHeadRequest(u.toURL());
        } catch (IOException|URISyntaxException e) {
            logger.warn("Exception for HEAD {}", url);
        }
        return code;
    }
    
    /**
     * Set proxy 
     */
    private static void setProxy() {
        String host = getProperty("proxy.host");
        String port = getProperty("proxy.port");
       
        if (!host.isEmpty()) {
            fetcher.setProxy(host, Integer.parseInt(port));
        }
    }
    
    /**
     * Parse file with URLS
     */
    private static void parse() {
        setProxy();
        
        String file = getProperty("urlfile");
        String outfile = getProperty("outfile");
        
        int skiplines = Integer.parseInt(getProperty("skiplines"));
        List<String> okcodes = Arrays.asList(getProperty("httpok").split(","));
       
        try (   BufferedReader r = Files.newBufferedReader(Paths.get(file));
                BufferedWriter w = Files.newBufferedWriter(Paths.get(outfile));) {
           
            logger.info("Loading urls from {}", file);
            
            String line = r.readLine();
            
            // skip header line(s)
            for(int i = 0; (i < skiplines) && (line != null); i++) {
                line = r.readLine();
            }
         
            int urlOK = 0;
            int urlBad = 0;
            int code = -1;
         
            while(line != null) {
                String s[] = line.split(";", 2);
                code = checkURL(s[0]);
                if (okcodes.contains(Integer.toString(code))) {
                    urlOK++;
                } else {
                    urlBad++;
                    w.write(code + ";" + line);
                    w.newLine();
                }
                line = r.readLine();
            }
            logger.info("Checked {} urls: {} ok, {} not ok", 
                                urlOK + urlBad, urlOK, urlBad);
        } catch (IOException ex) {
            logger.error("Error loading file", ex);
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
        
        parse();
        
        exit(0);
    }
}
