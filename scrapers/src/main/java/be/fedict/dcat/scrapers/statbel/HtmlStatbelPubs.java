/*
 * Copyright (c) 2017, FPS BOSA DG DT
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
package be.fedict.dcat.scrapers.statbel;

import be.fedict.dcat.scrapers.Page;
import be.fedict.dcat.helpers.Storage;
import be.fedict.dcat.scrapers.Cache;
import be.fedict.dcat.scrapers.Html;
import be.fedict.dcat.vocab.MDR_LANG;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Statbel "publications" scraper.
 *
 * @see https://statbel.fgov.be/
 * @author Bart Hanssens
 */
public class HtmlStatbelPubs extends Html {

    public final static String LINK_THEME = "nav.block--menu--themes-doormat ul.nav h3 a";
    public final static String NAV_SUBTHEME = "nav.block--menu--themes-doormat ul.nav";
    public final static String LINK_SUBTHEME = "h3 a";
    public final static String LINK_SUBSUBTHEME = "ul.menu li a";
	public final static String LI = "ul > li";
    public final static String DIV_DOCUMENT = "div.field--name-field-document-description";
    public final static String DIV_SUMMARY = "div.field--name-body";
    public final static String DIV_FILES = "div.field--name-field-document a";
    public final static String LI_THEMES = "ol.breadcrumb li a";

	public final static String LANG_LINK = "nav.language-switcher-language-url ul li a";
	
	/**
	 * Get the URL of the page in another language
	 *
	 * @param page
	 * @param lang
	 * @return URL of the page in another language
	 * @throws IOException
	 */
	private URL switchLanguage(String page, String lang) throws IOException {
		Elements hrefs = Jsoup.parse(page).select(LANG_LINK);

		for (Element href : hrefs) {
			if (href.text().trim().toLowerCase().equals(lang)) {
				String link = href.attr(HTML.Attribute.HREF.toString());
				if (link != null && !link.isEmpty()) {
					return makeAbsURL(link);
				}
			}
		}
		logger.warn("No {} translation for page", lang);
		return null;
	}

	/**
	 * Scrape dataset
	 *
	 * @param u
	 * @throws IOException
	 */
	@Override
	protected void scrapeDataset(URL u) throws IOException {
		Cache cache = getCache();
		String deflang = getDefaultLang();
		String html = makeRequest(u);

		cache.storePage(u, deflang, new Page(u, html));

		String[] langs = getAllLangs();
		for (String lang : langs) {
			if (!lang.equals(deflang)) {
				URL url = switchLanguage(html, lang);
				if (url != null) {
					String body = makeRequest(url);
					cache.storePage(u, lang, new Page(url, body));
				}
				sleep();
			}
		}
	}

    /**
     * Scrape URLs from subthemes
     *
     * @param u link to subtheme
     * @return list of URLs
     * @throws IOException
     */
    private List<URL> scrapeSubList(String u) throws IOException {
        List<URL> urls = new ArrayList<>();
        String subtheme = makeRequest(makeAbsURL(u));

        Elements nav = Jsoup.parse(subtheme).select(NAV_SUBTHEME);
        if (nav == null || nav.isEmpty()) {
            logger.warn("No subtheme element found");
            return urls;
        }

		Elements lis = nav.select(LI);
		if (lis == null || lis.isEmpty()) {
	        logger.warn("No subtheme links found");
            return urls;
    	}
		for (Element li: lis) {
			// Check if there is a third level of themes
			Elements subs = li.select(LINK_SUBSUBTHEME);
			if (subs != null && !subs.isEmpty()) {
				logger.debug("Subsubtheme elements {} for {}", subs.size(), u);
				for (Element sub : subs) {
					String href = sub.attr(Attribute.HREF.toString());
					if (!href.startsWith("https://indicators")) {
						urls.add(makeAbsURL(href));
					}
				}
			} else {
				// Not the case, so only the title points to a dataset
				logger.info("No subsubtheme elements for {}", u);
				Element link = li.select(LINK_SUBTHEME).first();
				if (link != null) {
					String href = link.attr(Attribute.HREF.toString());
					if (!href.startsWith("https://indicators")) {
						urls.add(makeAbsURL(href));
					}
				} else {
					logger.warn("No subthemes nor subsubthemes found {}", u);
				}
			}
		}
        return urls;
    }

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
        String front = makeRequest(base);

        // Get all the main themes
        Elements themes = Jsoup.parse(front).select(LINK_THEME);

        if (themes != null) {
            for (Element theme : themes) {
                String href = theme.attr(Attribute.HREF.toString());
                urls.addAll(scrapeSubList(href));
                sleep();
            }
        } else {
            logger.error("No themes {} found", LINK_THEME);
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

        // important for EDP: does not like different datasets pointing to same distribution
        String id = makeHashId(dataset.toString()) + "/" + makeHashId(download.toString());
        IRI dist = store.getURI(makeDistURL(id).toString() + "/" + lang);
        logger.debug("Generating distribution {}", dist.toString());

        store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
        store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
        store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
        store.add(dist, DCTERMS.TITLE, link.ownText(), lang);
        store.add(dist, DCAT.ACCESS_URL, access);
        store.add(dist, DCAT.DOWNLOAD_URL, download);
        store.add(dist, DCAT.MEDIA_TYPE, getFileExt(href));
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
            Element h = doc.getElementsByTag(Tag.H1.toString()).first();
            if (h == null) {
                logger.warn("No H1 element");
                continue;
            }
            String title = h.text();
            // by default, also use the title as description
            String desc = title;

            Element divmain = doc.select(DIV_DOCUMENT).first();
            if (divmain != null) {
                desc = divmain.text();
            } else {
                divmain = doc.select(DIV_SUMMARY).first();
                if (divmain != null) {
                    desc = divmain.text();
                } else {
                    logger.warn("No description found");
                    logger.warn(title);
                }
            }

            store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
            store.add(dataset, DCTERMS.TITLE, title, lang);
            store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);

            Elements themes = doc.select(LI_THEMES);
            if (themes != null && themes.size() > 2) {
                //ignore Home > Themes
                themes.remove(0);
                themes.remove(0);

                for (Element theme : themes) {
                    store.add(dataset, DCAT.KEYWORD, theme.text().trim(), lang);
                }
            } else {
                logger.warn("No themes found {}", LI_THEMES);
            }

            Elements links = doc.select(DIV_FILES);
            if (links != null) {
                for (Element link : links) {
                    generateDist(store, dataset, p.getUrl(), link, lang);
                }
            } else {
                logger.warn("No downloads found {}", DIV_FILES);
            }
        }
    }

    /**
     * HTML parser for Statbel publications
     *
     * @param prop
	 * @throws IOException
     */
    public HtmlStatbelPubs(Properties prop) throws IOException {
        super(prop);
        setName("statbelpubs");
    }
}
