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
import be.fedict.dcat.helpers.Page;
import be.fedict.dcat.helpers.Storage;
import be.fedict.dcat.vocab.DCAT;
import be.fedict.dcat.vocab.MDR_LANG;
import com.google.common.collect.ListMultimap;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenDataSoft City of Brussels / GIAL scraper.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class OdsBxlCity extends Ods {
    private final Logger logger = LoggerFactory.getLogger(OdsBxlCity.class);

    @Override
    protected void scrapeCat(Cache cache) throws IOException {
        URL front = getBase();
        URL url = new URL(getBase(), Ods.API_DCAT);
        String content = makeRequest(url);
        cache.storePage(front, "all", new Page(url, content));
    }
    
        
    /**
     * Generate DCAT catalog information.
     * 
     * @param store
     * @param catalog
     * @throws RepositoryException 
     */
    @Override
    public void generateCatalogInfo(Storage store, URI catalog) 
                                                    throws RepositoryException {
        super.generateCatalogInfo(store, catalog);
        store.add(catalog, DCTERMS.TITLE, "DCAT export BXL City", "en");
        store.add(catalog, DCTERMS.LANGUAGE, MDR_LANG.NL);
        store.add(catalog, DCTERMS.LANGUAGE, MDR_LANG.FR);
        store.add(catalog, DCTERMS.LANGUAGE, MDR_LANG.EN);
    }
   
    /**
     * Generate DCAT catalog
     * 
     * @param store
     * @throws MalformedURLException 
     * @throws RepositoryException
     */
    @Override
    public void generateCatalog(Storage store) throws MalformedURLException, RepositoryException {        
        // Replace BXL DCAT catalog URI with data.gov.be's
        URI page = store.getURI(new URL(getBase(), Ods.API_DCAT).toString());
        URI cat = store.getURI(makeCatalogURL().toString());
        store.replaceSubj(page, cat);
    
    }
    /**
     * Generate DCAT file
     * 
     * @param cache
     * @param store
     * @throws RepositoryException
     * @throws MalformedURLException 
     */
    @Override
    public void generateDcat(Cache cache, Storage store) 
                            throws RepositoryException, MalformedURLException {
        Map<String, Page> map = cache.retrievePage(getBase());
        String ttl = map.get("all").getContent();
        
        // Load turtle file into store
        try(InputStream in = new ByteArrayInputStream(ttl.getBytes())) {
            store.add(in, RDFFormat.TURTLE);
        } catch (RDFParseException | IOException ex) {
            throw new RepositoryException(ex);
        }
        generateCatalog(store);
    }
    
    /**
     * Constructor
     * 
     * @param caching
     * @param storage
     * @param base 
     */
    public OdsBxlCity(File caching, File storage, URL base) {
        super(caching, storage, base);
    }
}
