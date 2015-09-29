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
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Split a property.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class SplitProperty extends Enhancer {
    private final Logger logger = LoggerFactory.getLogger(SplitProperty.class);
    
    /**
     * Split property into multiple properties.
     * 
     * @param prop
     * @param sep
     * @throws RepositoryException 
     */
    private void split(URI prop, String sep) throws RepositoryException {
        logger.info("Split {} using separator {}", prop.toString(), sep);
        
        getStore().splitValues(prop, sep);
    }
    
    @Override
    public void enhance() {
        try {
            URI property = getStore().getURI(getProperty("property"));
            String sep = getProperty("separator");
            split(property, sep);
        } catch (RepositoryException ex) {
            logger.error("Repository error", ex);
        }
    }
    
    /**
     * Split property into multiple properties.
     * 
     * @param store 
     */
    public SplitProperty(Storage store) {
        super(store);
    }
}
