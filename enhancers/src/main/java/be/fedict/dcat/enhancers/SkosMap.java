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
import java.io.FileReader;
import java.io.IOException;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Map a literal from one property to an URI of another property.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class SkosMap extends Enhancer {
    private final Logger logger = LoggerFactory.getLogger(SkosMap.class);
    
    /**
     * Map literal using SKOS file.
     * 
     * @param prop
     * @param newprop
     * @param skosprop SKOS property used for mapping
     * @throws RepositoryException 
     */
    private void skosMap(URI prop, URI newprop, URI skosprop) 
                                                throws RepositoryException {
        logger.info("Mapping {} to {}", prop.toString(), newprop.toString());
        
        getStore().skosMap(prop, newprop, skosprop);
    }
    
    @Override
    public void enhance() {
        try {
            URI property = getStore().getURI(getProperty("property"));
            URI newproperty = getStore().getURI(getProperty("newproperty"));
            String skos = getProperty("skosfile");
            URI skosProp = getStore().getURI(getProperty("skosproperty"));
            getStore().read(new BufferedReader(new FileReader(skos)), RDFFormat.TURTLE);
            skosMap(property, newproperty, skosProp);
        } catch (RepositoryException ex) {
            logger.error("Repository error", ex);
        } catch (IOException ex) {
            logger.error("I/O error", ex);
        } catch (RDFParseException ex) {
            logger.error("Parse error", ex);
        }
    }
    
    /**
     * Map a literal from one property to an URI of another property.
     * 
     * @param store 
     */
    public SkosMap(Storage store) {
        super(store);
    }
}
