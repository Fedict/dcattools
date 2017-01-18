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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryException;

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

    public final static String ACCESS = "questionformnameurl_url";
    public final static String ACCESS2 = "publicinfo_siteurl";
    public final static String CREATED = "publishstartdate";
    public final static String DESC = "formshortdsc_dsc";
    public final static String DESC2 = "formcomment_comment";
    public final static String DESC3 = "datescomment_comment";
    public final static String DESC4 = "licensecomment_comment";
    public final static String DESC5 = "reusablebylicence_reglobasisname";
    public final static String DOWNLOAD = "forminformationurl_url";
    public final static String FEE = "feerequired";
    public final static String FMT1 = "efomrat_imageformatlabel";
    public final static String FMT2 = "eformat_vectorelformatlabel";
    public final static String FMT3 = "eformat_pageformatlabel";
    public final static String FREQID = "idfrequencytype_fk";        
    public final static String KEYWORD = "searchoninfo_";
    public final static String ORGID = "idinstitudiont_fk";
    public final static String PFROM = "dispdate";
    public final static String PTILL = "lastrevdate";
    public final static String REUSE = "reusablebylicence";
    public final static String RIGHTS = "obtentionurl_url";
    public final static String TITLE = "formtitle";

    public final static DateFormat DATEFMT = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    protected URL getId(Row row) throws MalformedURLException {
        String s = row.getCell(0).toString();
        return makeDatasetURL(stringInt(s));
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
                date = XlsPsiBelgium.DATEFMT.parse(s);
            } catch (ParseException ex) {
                logger.warn("Could not parse {} to date", s);
            }
        }
        return date;
    }
    
    /**
     * Get array of keywords, based on last part of search field.
     * 
     * @param map
     * @param lang language code
     * @return 
     */
    protected String[] getKeywords(Map<String,String> map, String lang) {
        String s = map.getOrDefault(XlsPsiBelgium.KEYWORD + lang, "");
        // Keywords are added after the last sentence of the description
        int pos = s.lastIndexOf('.');
        if (pos > 0) {
            s = s.substring(pos + 1);
            return s.replaceAll("\n", "").split(",");
        }
        return (new String[0]);
    }
    
    /**
     * Get array of formats
     * 
     * @param map
     * @return string array of formats
     */
    private String[] getFormats(Map<String,String> map) {
        String fmt1 = map.getOrDefault(XlsPsiBelgium.FMT1, "");
        String fmt2 = map.getOrDefault(XlsPsiBelgium.FMT2, "");
        String fmt3 = map.getOrDefault(XlsPsiBelgium.FMT3, "");
        
        String fmt = fmt1 + "," + fmt2 + "," + fmt3;
        return fmt.toLowerCase().replace(";", ",").replace(".", "").split(",");
    }
    
    /**
     * Add organization / publisher
     * 
     * @param store
     * @param dataset
     * @param s
     * @throws MalformedURLException
     * @throws RepositoryException 
     */
    private void generateOrg(Storage store, IRI dataset, Map<String,String> map) 
                            throws MalformedURLException, RepositoryException {
        String s = map.getOrDefault(XlsPsiBelgium.ORGID, "");
        IRI org = store.getURI(makeOrgURL(stringInt(s)).toString());
        store.add(dataset, DCTERMS.PUBLISHER, org);
        store.add(org, RDF.TYPE, FOAF.ORGANIZATION);
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
    private void generateDist(Storage store, IRI dataset, Map<String,String> map,
            String id, String lang) throws RepositoryException, MalformedURLException {
        URL u  = makeDistURL(id + "/" + lang);
        IRI dist = store.getURI(u.toString());
        logger.debug("Generating distribution {}", dist.toString());

        String access = map.getOrDefault(XlsPsiBelgium.ACCESS + lang, "");
        String access2 = map.getOrDefault(XlsPsiBelgium.ACCESS2 + lang, "");
        
        String rights = map.getOrDefault(XlsPsiBelgium.RIGHTS + lang, "");
        String download = map.getOrDefault(XlsPsiBelgium.DOWNLOAD + lang, "");
/*
        if (!fmt.equals("")) {
            store.add(dist, DCAT.MEDIA_TYPE, fmt.trim());
        }
  */      
        String fee = stringInt(map.get(XlsPsiBelgium.FEE));
        String reuse = stringInt(map.get(XlsPsiBelgium.REUSE));
        boolean open = reuse.equals("1") && fee.equals("0");
        
        store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
        store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
        store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
        if(!access.isEmpty()) {
            store.add(dist, DCAT.ACCESS_URL, new URL(access));
        }
        if (!access2.isEmpty()) {
            store.add(dist, DCAT.ACCESS_URL, new URL(access2));
        }
        if (!download.isEmpty()) {
            store.add(dist, DCAT.DOWNLOAD_URL, new URL(download));
            store.add(dist, DCAT.MEDIA_TYPE, getFileExt(download));
        }
        store.add(dist, DCTERMS.LICENSE, open ? "open" : "closed");
        if (!rights.isEmpty()) {
            store.add(dist, DCTERMS.RIGHTS, new URL(rights));
        }
    }
    
    /**
     * Generate DCAT Dataset.
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
        IRI dataset = store.getURI(u.toString());  
        logger.debug("Generating dataset {}", dataset.toString());
        
        String id = stringInt(map.get(XlsPsiBelgium.ID));
        String freq = stringInt(map.getOrDefault(XlsPsiBelgium.FREQID, "0"));
        
        store.add(dataset, RDF.TYPE, DCAT.DATASET);
        store.add(dataset, DCTERMS.IDENTIFIER, makeHashId(u.toString()));
        
        Date created = getDate(map, XlsPsiBelgium.CREATED);
        if (created != null) {
            store.add(dataset, DCTERMS.CREATED, created);
        }
        
        store.add(dataset, DCTERMS.ACCRUAL_PERIODICITY, freq);
        
        String[] langs = getAllLangs();
        for (String lang : langs) {
            String title = map.getOrDefault(XlsPsiBelgium.TITLE + lang, "");
            
            String desc = map.getOrDefault(XlsPsiBelgium.DESC + lang, title);
            String desc2 = map.getOrDefault(XlsPsiBelgium.DESC2 + lang, "");
            if (!desc2.isEmpty()) {
                desc = desc + "\n\n" + desc2;
            }
            String desc3 = map.getOrDefault(XlsPsiBelgium.DESC3 + lang, "");
            if (!desc3.isEmpty()) {
                desc = desc + "\n\n" + desc3;
            }
            String desc4 = map.getOrDefault(XlsPsiBelgium.DESC4 + lang, "");
            if (!desc4.isEmpty()) {
                desc = desc + "\n\n" + desc4;
            }
            String desc5 = map.getOrDefault(XlsPsiBelgium.DESC5 + lang, "");
            if (!desc5.isEmpty()) {
                desc = desc + "\n\n" + desc5;
            }
            
            store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
            store.add(dataset, DCTERMS.TITLE, title, lang);
            store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
            
            String from = stringInt(map.getOrDefault(XlsPsiBelgium.PFROM, ""));
            String till = stringInt(map.getOrDefault(XlsPsiBelgium.PTILL, ""));
            String period = from + " / " + till;
            if (! period.equals(" / ")) {
                store.add(dataset, DCTERMS.TEMPORAL, period);
            }
            
            String[] words = getKeywords(map, lang);
            for (String word : words) {
                store.add(dataset, DCAT.KEYWORD, word.trim(), lang);
            }
            
			generateDist(store, dataset, map, id, lang);
        }
        generateOrg(store, dataset, map);
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
