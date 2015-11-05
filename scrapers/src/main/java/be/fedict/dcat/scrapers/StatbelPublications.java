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

package be.fedict.dcat.scrapers;

import be.fedict.dcat.helpers.Cache;
import be.fedict.dcat.helpers.Storage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Statbel "publications" scraper.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class StatbelPublications extends Html {
    private final Logger logger = LoggerFactory.getLogger(StatbelPublications.class);
    
    @Override
    public URL switchLanguage(String lang) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void generateCatalogInfo(Storage store, URI catalog) 
                                                    throws RepositoryException {
        super.generateCatalogInfo(store, catalog);
        store.add(catalog, DCTERMS.TITLE, "Statbel downloads", "en");
    }

    @Override
    public void generateDatasets(Map<String, String> page, Storage store) throws MalformedURLException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    

   /**
     * Store front page containing list of datasets
     * 
     * @param cache 
     * @throws java.io.IOException 
     */
    @Override
    public void scrapeFront(Cache cache) throws IOException {
        URL front = getBase();
        
        for (String lang : getAllLangs()) {
            URL url = switchLanguage(lang);
            cache.storePage(front, makeRequest(url), lang);
        }
    }

    /**
     * HTML parser for Statbel publications
     * 
     * @param caching
     * @param storage
     * @param base 
     */
    public StatbelPublications(File caching, File storage, URL base) {
        super(caching, storage, base);
        setDefaultLang("nl");
    }
}
