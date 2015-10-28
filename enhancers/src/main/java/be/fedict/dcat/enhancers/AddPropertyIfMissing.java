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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add a property to a class if the property is missing. 
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class AddPropertyIfMissing extends Enhancer {
    private final Logger logger = LoggerFactory.getLogger(AddPropertyIfMissing.class);

    public final static Pattern LITERAL = Pattern.compile("\"(.*)\"@(\\w*)");
    /**
     * Add property if it is missing
     * 
     * @param rdfClass
     * @param prop
     * @param value url or string value
     * @throws RepositoryException 
     */
    private void addIfMissing(URI rdfClass, URI prop, String value) throws RepositoryException {
        logger.info("Add missing {} to {}", prop.toString(), rdfClass.toString());
        
        URL url = null;
        String str = "";
        String lang = "";
        // Check if the value is an url or a literal
        try {
            url = new URL(value);
        } catch (MalformedURLException ex) {
            Matcher m = LITERAL.matcher(value);
            if (m.find()) {
                str = m.group(1);
                lang = m.group(2);
            }
        }
        
        Storage store = getStore();
        List<URI> subjs = store.query(rdfClass);
        int added = 0;
        for (URI subj : subjs) {
            if (! store.has(subj, prop)) {
                if (url != null) {
                    store.add(subj, prop, url);
                } else {
                    store.add(subj, prop, str, lang);
                }
                added++;
            }
        }
        logger.info("Property added {} times", Integer.toString(added));
    } 
    
    @Override
    public void enhance() {
        try {
            URI rdfClass = getStore().getURI(getProperty("rdfclass"));
            URI property = getStore().getURI(getProperty("property"));
            String value = getProperty("value");
            
            addIfMissing(rdfClass, property, value);
        } catch (RepositoryException ex) {
            logger.error("Repository error", ex);
        }
    }
    
    /**
     * Adds a default property if the property is missing.
     * 
     * @param store 
     */
    public AddPropertyIfMissing(Storage store) {
        super(store);
    }
}
