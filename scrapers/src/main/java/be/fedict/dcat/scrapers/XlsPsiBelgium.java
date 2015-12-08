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
import be.fedict.dcat.vocab.DCAT;
import be.fedict.dcat.vocab.MDR_LANG;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scraper for PSI Belgium Excel export.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class XlsPsiBelgium extends Xls {
    private final Logger logger = LoggerFactory.getLogger(XlsPsiBelgium.class);
        
    public final static String ID = "contentid";
    public final static String VER = "version";
    public final static String TITLE = "formtitle";
    public final static String DESC = "formshortdsc_dsc";
    public final static String ACCESS = "questionformnameurl_url";
    public final static String DOWNLOAD = "forminformationurl_url";
    


    @Override
    protected URL getId(Row row) throws MalformedURLException {
        String s = row.getCell(0).toString();
        if (!s.isEmpty() && s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        return makeDatasetURL(getName() + "/" + s);
    }

    
    /**
     * 
     * @param store
     * @param dataset
     * @param map
     * @param lang
     * @throws RepositoryException 
     */
    private void generateDist(Storage store, URI dataset, Map<String,String> map,
            String id, String lang) throws RepositoryException, MalformedURLException {
        URL u  = makeDistURL(getName() + "/" + id + "/" + lang);
        URI dist = store.getURI(u.toString());
        logger.debug("Generating distribution {}", dist.toString());

        String access = map.getOrDefault(XlsPsiBelgium.ACCESS, "http://");
        String download = map.getOrDefault(XlsPsiBelgium.DOWNLOAD, "http://");
        
        store.add(dataset, DCAT.DISTRIBUTION, dist);
        store.add(dist, RDF.TYPE, DCAT.A_DISTRIBUTION);
        store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
        store.add(dist, DCAT.ACCESS_URL, access);
        
        if (!download.equals("http://")) {
            store.add(dist, DCAT.DOWNLOAD_URL, download);
            store.add(dist, DCAT.MEDIA_TYPE, getFileExt(download));
        }
    }
    
    /**
     * Generate DCAT dataset
     * 
     * @param store
     * @param map 
     * @param u
     * @throws RepositoryException
     * @throws MalformedURLException
     */
    @Override
    public void generateDataset(Storage store, Map<String,String> map, URL u) 
                            throws RepositoryException, MalformedURLException {
        URI dataset = store.getURI(u.toString());  
        logger.debug("Generating dataset {}", dataset.toString());
        
        store.add(dataset, RDF.TYPE, DCAT.A_DATASET);
        store.add(dataset, DCTERMS.IDENTIFIER, makeHashId(u.toString()));
        
        String[] langs = getAllLangs();
        for (String lang : langs) {
            String id = map.get(ID);
            String title = map.getOrDefault(XlsPsiBelgium.TITLE + lang, "");
            String desc = map.getOrDefault(XlsPsiBelgium.DESC + lang, title);
            
            store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
            store.add(dataset, DCTERMS.TITLE, title, lang);
            store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);        
            
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
        store.add(catalog, DCTERMS.TITLE, "DCAT export PSI Belgium", "en");
        store.add(catalog, DCTERMS.LANGUAGE, MDR_LANG.NL);
        store.add(catalog, DCTERMS.LANGUAGE, MDR_LANG.FR);
    }
    
    @Override
    public void generateDcat(Cache cache, Storage store) 
                            throws RepositoryException, MalformedURLException {
        logger.info("Generate DCAT");
        
        /* Get the list of all datasets */
        List<URL> urls = cache.retrieveURLList();
        for (URL u : urls) {
            Map<String,String> map = cache.retrieveMap(u);
            generateDataset(store, map, u);
        }
        generateCatalog(store);
    }
    
    /**
     * Constructor.
     * 
     * @param caching
     * @param storage
     * @param base 
     */
    public XlsPsiBelgium(File caching, File storage, URL base) {
        super(caching, storage, base);
        setName("psibelgium");
    }
}
