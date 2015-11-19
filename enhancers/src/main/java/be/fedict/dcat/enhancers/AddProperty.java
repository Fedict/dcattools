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
public class AddProperty extends Enhancer {
    private final Logger logger = LoggerFactory.getLogger(AddProperty.class);

    public final static Pattern LITERAL = Pattern.compile("\"(.*)\"@(\\w*)");
        
    /**
     * Checks if adding property is really needed.
     * Override this method in derived classes.
     * 
     * @param store
     * @param subj
     * @param prop
     * @return always true
     * @throws RepositoryException 
     */
    public boolean isNeeded(Storage store, URI subj, URI prop) throws RepositoryException {
        return true;
    }
    
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
        
        Storage store = getStore();
        
        URI uri = null;
        String str = "";
        String lang = "";
        
        // Check if the value is an url/mail address or a literal
        if (value.startsWith("http://") || value.startsWith("https://") 
                                        || value.startsWith("mailto:")) {
            uri = store.getURI(value);
        } else {
            Matcher m = LITERAL.matcher(value);
            if (m.find()) {
                str = m.group(1);
                lang = m.group(2);
            }
            logger.error("No Resource nor Literal: {}", value);
        }
    
        List<URI> subjs = store.query(rdfClass);
        int added = 0;
        for (URI subj : subjs) {
            if (isNeeded(store, subj, prop)) {
                if (uri != null) {
                    store.add(subj, prop, uri);
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
    public AddProperty(Storage store) {
        super(store);
    }
}
