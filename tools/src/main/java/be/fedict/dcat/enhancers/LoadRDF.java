/*
 * Copyright (c) 2015, FPS BOSA DG DT
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load/import an RDF file.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class LoadRDF extends Enhancer {
    private final Logger logger = LoggerFactory.getLogger(LoadRDF.class);
    
    @Override
    public void enhance() {
        try {
            String file = getProperty("rdffile");
            getStore().read(new BufferedReader(
                            new InputStreamReader(
                            new FileInputStream(file),  StandardCharsets.UTF_8)),
                            RDFFormat.TURTLE);
        } catch (RepositoryException|IOException|RDFParseException ex) {
            logger.error("Error loading file", ex);
        }
    }
    
    /**
     * Map a literal from one property to an URI of another property.
     * 
     * @param store 
     */
    public LoadRDF(Storage store) {
        super(store);
    }
}
