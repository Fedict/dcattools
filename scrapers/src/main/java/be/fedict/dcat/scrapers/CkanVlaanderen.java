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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.repository.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CKAN Corve / Vlaanderen BZ.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class CkanVlaanderen extends CkanJson {
    private final Logger logger = LoggerFactory.getLogger(CkanVlaanderen.class);
 
    public final static String CONFORM = "conformity-specification-title";
    public final static String CONTACT = "contact-email";
    public final static String DOMAIN = "beleidsdomein";
    public final static String FREQ = "frequency-of-update";
    public final static String GEMET = "gemet-theme";
    public final static String GEOCOVERAGE = "geografische dekking";
    public final static String METADATA_REQ = "metadata_request";
    public final static String RESPONSABLE = "responsible-party";
    
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
    protected void ckanExtras(Storage store, IRI uri, JsonObject json, String lang) throws RepositoryException, MalformedURLException {
        JsonArray arr = json.getJsonArray(CkanJson.EXTRA);
        if (arr == null) {
            return;
        }
        for (JsonObject obj : arr.getValuesAs(JsonObject.class)) {
            String key = obj.getString(CkanJson.KEY, "");
            switch(key) {
                case CkanVlaanderen.DOMAIN:
                    parseString(store, uri, obj, CkanJson.VALUE, DCAT.KEYWORD, lang);
                    break;
                case CkanVlaanderen.FREQ:
                    parseString(store, uri, obj, CkanJson.VALUE, DCTERMS.ACCRUAL_PERIODICITY, null);
                    break;
                case CkanVlaanderen.GEMET:
                    parseString(store, uri, obj, CkanJson.VALUE, DCAT.KEYWORD, lang);
                    break;
                case CkanVlaanderen.GEOCOVERAGE:
                    parseString(store, uri, obj, CkanJson.VALUE, DCTERMS.COVERAGE, lang);
                    break;
                case CkanVlaanderen.METADATA_REQ:
                    parseURI(store, uri, obj, CkanJson.VALUE, DCAT.LANDING_PAGE);
                    break;
                case CkanVlaanderen.CONTACT:
                    parseContact(store, uri, obj, CkanJson.VALUE, "", DCAT.CONTACT_POINT);
                    break;
                case CkanVlaanderen.RESPONSABLE:
                    parseContact(store, uri, obj, CkanJson.VALUE, "", DCAT.CONTACT_POINT);
                    break;
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
        setName("vlaanderen");
    }
}
