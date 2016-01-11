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
import be.fedict.dcat.vocab.MDR_LANG;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class XlsStatbelOpen extends Xls {
    private final Logger logger = LoggerFactory.getLogger(XlsStatbelOpen.class);
  
    public final static DateFormat DATEFMT = 
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public final static String ID = "dcat:dataset";
    public final static String TITLE = "dct:title";
    public final static String CREATED = "dcat:issued";
    public final static String DESC = "dct:description";
    
    public final static String DOWNLOAD = "dcat:downloadurl,dct:format";
            
    public final static String[] FMTS = { "xlsx", "txt" };
    
    @Override
    protected URL getId(Row row) throws MalformedURLException {
        String s = row.getCell(0).toString();
        return makeDatasetURL(s);
    }

      /**
     * Get date from field
     * 
     * @param map
     * @param field 
     * @return date or null
     */
    protected Date getDate(Map<String,String> map, String field) {
        Date date = null;
        String s = map.getOrDefault(field, "");
        if (!s.isEmpty()) {
            try {
                date = XlsStatbelOpen.DATEFMT.parse(s);
            } catch (ParseException ex) {
                logger.warn("Could not parse {} to date", s);
            }
        }
        return date;
    }
  
    /**
     * Generate DCAT Distribution.
     * 
     * @param store
     * @param dataset
     * @param map
     * @param lang
     * @throws RepositoryException 
     */
    private void generateDist(Storage store, URI dataset, Map<String,String> map,
            String id, String lang) throws RepositoryException, MalformedURLException {
    
        for(String fmt: FMTS) {
            URL u  = makeDistURL(id + "/" + lang + "/" + fmt);
            URI dist = store.getURI(u.toString());
            logger.debug("Generating distribution {}", dist.toString());

            String download = map.getOrDefault(
                        XlsStatbelOpen.DOWNLOAD + "<" + fmt + ">@" + lang, "");
        
            if (! download.isEmpty()) {
                store.add(dataset, DCAT.DISTRIBUTION, dist);
                store.add(dist, RDF.TYPE, DCAT.A_DISTRIBUTION);
                store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
                store.add(dist, DCAT.DOWNLOAD_URL, download);
                store.add(dist, DCAT.MEDIA_TYPE, fmt);
            }
        }
    }
    
    @Override
    public void generateDataset(Storage store, Map<String, String> map, URL u) 
            throws RepositoryException, MalformedURLException {
        URI dataset = store.getURI(u.toString());  
        logger.debug("Generating dataset {}", dataset.toString());
        
        store.add(dataset, RDF.TYPE, DCAT.A_DATASET);
        store.add(dataset, DCTERMS.IDENTIFIER, makeHashId(u.toString()));
        
        String[] langs = getAllLangs();
        for (String lang : langs) {
            String id = map.get(ID);
            String title = map.getOrDefault(XlsStatbelOpen.TITLE + "@" + lang, "");
            String desc = map.getOrDefault(XlsStatbelOpen.DESC + "@" + lang, title);
            
            store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
            store.add(dataset, DCTERMS.TITLE, title, lang);
            store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
            
            Date created = getDate(map, XlsStatbelOpen.CREATED);
            if (created != null) {
                store.add(dataset, DCTERMS.CREATED, created);
            }
            
            generateDist(store, dataset, map, id, lang);
        }
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
        store.add(catalog, DCTERMS.TITLE, "DCAT export Statbel open data", "en");
        store.add(catalog, DCTERMS.LANGUAGE, MDR_LANG.NL);
        store.add(catalog, DCTERMS.LANGUAGE, MDR_LANG.FR);
    }
    
    /**
     * Constructor.
     * 
     * @param caching
     * @param storage
     * @param base 
     */
    public XlsStatbelOpen(File caching, File storage, URL base) {
        super(caching, storage, base);
        setName("statbelopen");
    }
}
