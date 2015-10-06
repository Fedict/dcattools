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
import be.fedict.dcat.vocab.DCAT;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.json.JsonArray;
import javax.json.JsonObject;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class CkanVlaanderen extends Ckan {
    private final Logger logger = LoggerFactory.getLogger(CkanVlaanderen.class);
 
    public final static String CONFORM = "conformity-specification-title";
    public final static String DOMAIN = "beleidsdomein";
    public final static String GEMET = "gemet-theme";
    public final static String GEOCOVERAGE = "geografische dekking";
    public final static String METADATA_REQ = "metadata_request";
    
    /**
     * Parse CKAN "extra" section.
     * 
     * @param store
     * @param uri
     * @param json
     * @throws RepositoryException
     * @throws MalformedURLException 
     */
    @Override
    protected void ckanExtras(Storage store, URI uri, JsonObject json, String lang) throws RepositoryException, MalformedURLException {
        JsonArray arr = json.getJsonArray(Ckan.EXTRA);
        if (arr == null) {
            return;
        }
        for (JsonObject obj : arr.getValuesAs(JsonObject.class)) {
            String key = obj.getString(Ckan.KEY, "");
            switch(key) {
                case CkanVlaanderen.DOMAIN:
                    parseString(store, uri, obj, Ckan.VALUE, DCAT.THEME, lang);
                    break;
                case CkanVlaanderen.GEMET:
                    parseString(store, uri, obj, Ckan.VALUE, DCAT.THEME, lang);
                    break;
                case CkanVlaanderen.GEOCOVERAGE:
                    parseString(store, uri, obj, Ckan.VALUE, DCTERMS.COVERAGE, lang);
                    break;
                case CkanVlaanderen.METADATA_REQ:
                    parseURI(store, uri, obj, Ckan.VALUE, DCAT.LANDING_PAGE);
                default:
                    break;
            }
        }
    }
    
    /**
     * CKAN parser for OpendataForum.info / Corve.
     * 
     * @param caching
     * @param storage
     * @param base 
     */
    public CkanVlaanderen(File caching, File storage, URL base) {
        super(caching, storage, base);
        this.setDefaultLang("nl");
    }
}
