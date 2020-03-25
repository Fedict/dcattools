/*
 * Copyright (c) 2017, Bart Hanssens <bart.hanssens@fedict.be>
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

import be.fedict.dcat.helpers.Page;
import be.fedict.dcat.helpers.Storage;
import be.fedict.dcat.vocab.MDR_LANG;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Attribute;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Bart.Hanssens
 */
public class HtmlStatbelOpen extends HtmlStatbel {
    public final static String VIEW_HREF = "div.view-open-data h2 a";

    public final static String P_DESC = "article.node div.field--name-body p";
    public final static String DIV_TEMP = "div.field--name-field-period-manual";
    public final static String DIV_CAT = "div.field--name-field-category div";
    public final static String DIV_FILES = "div.field--name-field-downloads a";

    public final static Pattern YEAR_PAT
            = Pattern.compile(".*((18|19|20)[0-9]{2}-(19|20)[0-9]{2}).*");

    /**
     * Get the list of all the downloads (DCAT Dataset).
     *
     * @return List of URLs
     * @throws IOException
     */
    @Override
    protected List<URL> scrapeDatasetList() throws IOException {
        List<URL> urls = new ArrayList<>();

        URL base = getBase();
        // Go through all the pages
        for (int i = 0;; i++) {
            logger.info("Scraping page {}", i);
            String page = makeRequest(new URL(base + "?page=" + i));

            Elements links = Jsoup.parse(page).select(VIEW_HREF);
            if (links == null || links.isEmpty()) {
                break;
            }
            for (Element link : links) {
                String href = link.attr(Attribute.HREF.toString());
                urls.add(makeAbsURL(href));
            }
            sleep();
        }
        return urls;
    }

    /**
     * Generate DCAT Distribution.
     *
     * @param store RDF store
     * @param dataset dataset URI
     * @param access access URL
     * @param link link element
     * @param lang language code
     * @throws MalformedURLException
     * @throws RepositoryException
     */
    private void generateDist(Storage store, IRI dataset, URL access, Element link,
            String lang) throws MalformedURLException, RepositoryException {
        String href = link.attr(Attribute.HREF.toString());
        URL download = makeAbsURL(href);

        String id = makeHashId(dataset.toString()) + "/" + makeHashId(download.toString());
        IRI dist = store.getURI(makeDistURL(id).toString() + "/" + lang);
        logger.debug("Generating distribution {}", dist.toString());

        store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
        store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
        store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
        store.add(dist, DCTERMS.TITLE, link.ownText(), lang);
        store.add(dist, DCAT.ACCESS_URL, access);
        store.add(dist, DCAT.DOWNLOAD_URL, download);
        store.add(dist, DCAT.MEDIA_TYPE, link.ownText());
    }

    /**
     * Generate DCAT Dataset.
     *
     * @param store RDF store
     * @param id
     * @param page
     * @throws MalformedURLException
     * @throws RepositoryException
     */
    @Override
    public void generateDataset(Storage store, String id, Map<String, Page> page)
            throws MalformedURLException, RepositoryException {

        IRI dataset = store.getURI(makeDatasetURL(id).toString());
        logger.info("Generating dataset {}", dataset.toString());

        store.add(dataset, RDF.TYPE, DCAT.DATASET);
        store.add(dataset, DCTERMS.IDENTIFIER, id);

        for (String lang : getAllLangs()) {
            Page p = page.get(lang);
            if (p == null) {
                logger.warn("Page {} not available in {}", dataset.toString(), lang);
                continue;
            }
            String html = p.getContent();

            Element doc = Jsoup.parse(html).body();
            if (doc == null) {
                logger.warn("No body element");
                continue;
            }
            Element h = doc.getElementsByTag(HTML.Tag.H1.toString()).first();
            if (h == null) {
                logger.warn("No H1 element");
                continue;
            }
            String title = h.text();
            // by default, also use the title as description
            String desc = title;

            Elements paras = doc.select(P_DESC);
            if (paras != null) {
                StringBuilder buf = new StringBuilder();
                for (Element para : paras) {
                    buf.append(para.text()).append('\n');
                }
                if (buf.length() == 0) {
                    buf.append(title);
                }
                desc = buf.toString();
            } else {
                logger.warn("No {} element", P_DESC);
            }

            Element t = doc.select(DIV_TEMP).first();
            if (t != null) {
                String temp = t.text().trim();
                if (temp.length() == 4) {
                    temp = temp + "-" + temp;
                }
                generateTemporal(store, dataset, temp, YEAR_PAT, "-");
            }
            store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
            store.add(dataset, DCTERMS.TITLE, title, lang);
            store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);

            Elements cats = doc.select(DIV_CAT);
            if (cats != null) {
                for (Element cat : cats) {
                    store.add(dataset, DCAT.KEYWORD, cat.text().toLowerCase().trim(), lang);
                }
            }

            Elements links = doc.select(DIV_FILES);
            for (Element link : links) {
                generateDist(store, dataset, p.getUrl(), link, lang);
            }
        }
    }

    /**
     * HTML parser for Statbel opendata publications
     *
     * @param caching DB cache file
     * @param storage RDF back-end
     * @param base base URL
     */
    public HtmlStatbelOpen(File caching, File storage, URL base) {
        super(caching, storage, base);
        setName("statbelopen");
    }
}
