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

import be.fedict.dcat.helpers.Storage;
import be.fedict.dcat.vocab.MDR_LANG;
import java.io.File;
import java.net.URL;
import javax.json.JsonObject;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CKAN Wallonie / AWT.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class CkanWallonie extends Ckan {
    private final Logger logger = LoggerFactory.getLogger(CkanWallonie.class);
 
    @Override
    protected void ckanExtras(Storage store, URI uri, JsonObject json, String lang) {
        // do nothing
    }
    
    @Override
    public void generateCatalogInfo(Storage store, URI catalog) 
                                                    throws RepositoryException {
        super.generateCatalogInfo(store, catalog);
        store.add(catalog, DCTERMS.TITLE, "DCAT export CKAN Wallonia", "en");
        store.add(catalog, DCTERMS.LANGUAGE, MDR_LANG.FR);
    }
    
   /**
    * CKAN parser for Opendata.DigitalWallonia.be / AWT.
    * 
    * @param caching
    * @param storage
    * @param base 
    */
    public CkanWallonie(File caching, File storage, URL base) {
        super(caching, storage, base);
        setName("ckanwal");
    }  
}
