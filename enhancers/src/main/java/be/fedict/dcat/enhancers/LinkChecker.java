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

import be.fedict.dcat.helpers.Fetcher;
import be.fedict.dcat.helpers.Storage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple link checker.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class LinkChecker extends Enhancer {
    private final static Logger logger = LoggerFactory.getLogger(LinkChecker.class);
   
    private final Fetcher fetcher = new Fetcher();
    
    /**
     * Check URL
     * 
     * @param url string
     * @return HTTP Status code or -1
     */
    private int checkURL(String url) {
        int code = -1;
        try {
            code = fetcher.makeHeadRequest(new URL(url));
        } catch (IOException e) {
            logger.warn("Exception for HEAD {}", url);
        }
        return code;
    }
    
    @Override
    public void enhance() {
        String file = getProperty("urlfile");
        String outfile = getProperty("outfile");
        
        String host = getProperty("proxy.host");
        String port = getProperty("proxy.port");
        
        if (!host.isEmpty()) {
            fetcher.setProxy(host, Integer.parseInt(port));
        }
        
        try (   BufferedReader r = Files.newBufferedReader(Paths.get(file));
                BufferedWriter w = Files.newBufferedWriter(Paths.get(outfile));) {
           
            logger.info("Loading urls from {}", file);
            
            String line = r.readLine();
            while(line != null) {
                String s[] = line.split(";", 2);
                w.write(checkURL(s[0]) + ";" + line);
                w.newLine();
                line = r.readLine();
            }
        } catch (IOException ex) {
            logger.error("Error loading file", ex);
        }
    }
    
    /**
     * Constructor
     * 
     * @param store 
     */
    public LinkChecker(Storage store) {
        super(store);
    }
}
