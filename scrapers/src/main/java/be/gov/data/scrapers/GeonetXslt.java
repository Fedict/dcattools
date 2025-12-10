/*
 * Copyright (c) 2025, FPS BOSA DG DT
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
package be.gov.data.scrapers;

import be.gov.data.helpers.Storage;
import static be.gov.data.scrapers.BaseScraper.PKG_PREFIX;
import static be.gov.data.scrapers.GeonetGmd.GMD;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;

/**
 * Abstract scraper for the GeonetRDF v3 portal software with DCAT export.
 *
 * @see http://geonetwork-opensource.org/
 *
 * @author Bart Hanssens
 */
public abstract class GeonetXslt extends Geonet {

	private final SAXReader sax;
	private final Transformer transformer;
	
	public final static String API = "/eng/csw?service=CSW&version=2.0.2";
	public final static int MAX_RECORDS = 200;
	public final static String API_RECORDS = API
		+ "&request=GetRecords&resultType=results"
		+ "&outputSchema=" + GMD
		+ "&elementSetName=full&typeNames=gmd:MD_Metadata"
		+ "&maxRecords=" + MAX_RECORDS;
	public final static String POSITION = "&startPosition=";

	public final static String NUM_REC = "csw:GetRecordsResponse/csw:SearchResults/@numberOfRecordsMatched";

	public final static String XP_DATASETS = "//gmd:MD_Metadata";

	private final static Map<String, String> NS = new HashMap<>();

	static {
		NS.put("csw", "http://www.opengis.net/cat/csw/2.0.2");
		NS.put("gmd", "http://www.isotc211.org/2005/gmd");
		NS.put("gmx", "http://www.isotc211.org/2005/gmx");
		NS.put("gml", "http://www.opengis.net/gml");
		NS.put("gml32", "http://www.opengis.net/gml/3.2");
		NS.put("gco", "http://www.isotc211.org/2005/gco");
		NS.put("srv", "http://www.isotc211.org/2005/srv");
		NS.put("xlink", "http://www.w3.org/1999/xlink");
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
		Set<URL> urls = cache.retrievePageList();

		try {
			for (URL url : urls) {
				Map<String, Page> map = cache.retrievePage(url);
				String xml = map.get("all").getContent();
				Document doc = sax.read(new StringReader(xml));
				List<Node> nodes = doc.selectNodes(XP_DATASETS);

				for (Node node: nodes) {
					StreamSource source = new StreamSource(new StringReader(node.asXML()));
					StringWriter sw = new StringWriter();
					StreamResult result = new StreamResult(sw);

					try {
						transformer.transform(source, result);
						StringReader sr = new StringReader(sw.toString());
						store.add(sr, RDFFormat.RDFXML);
					} catch (TransformerException tex) {
						LOG.error(tex.getMessage());
					}
				}
			}
		} catch (DocumentException|IOException ex) {
			LOG.error("Error parsing XML");
			throw new RepositoryException(ex);
		}

		generateCatalog(store);
	}

	/**
	 * Get total number of results from XML response
	 *
	 * @param doc
	 * @return number of records
	 */
	private int getNumRecords(Document doc) {
		Node rec = doc.selectSingleNode(NUM_REC);
		if (rec != null) {
			String n = rec.getText();
			LOG.info(n + " records found");
			return Integer.parseInt(n);
		}
		return MAX_RECORDS;
	}

	/**
	 * Scrape DCAT catalog.
	 *
	 * @param cache
	 * @throws IOException
	 */
	protected void scrapeCat(Cache cache) throws IOException {
		String prevhash = "";
				
		for (int pos = 1, recs = MAX_RECORDS; pos < recs; pos += MAX_RECORDS) {
			URL url = new URL(getBase() + API_RECORDS + POSITION + pos);
			String xml = makeRequest(url);
			
			prevhash = detectLoop(prevhash, xml);

			try {
				Document doc = sax.read(new StringReader(xml));
				if (pos == 1) {
					recs = getNumRecords(doc);
				}
				cache.storePage(url, "all", new Page(url, xml));
			} catch (DocumentException ex) {
				LOG.error("Error parsing XML " + url);
			}
		}			
	}

	/**
	 * Scrape DCAT catalog.
	 *
	 * @throws IOException
	 */
	@Override
	public void scrape() throws IOException {
		LOG.info("Start scraping");
		Cache cache = getCache();

		Set<URL> urls = cache.retrievePageList();
		if (urls.isEmpty()) {
			scrapeCat(cache);
		}
		LOG.info("Done scraping");
	}

	/**
	 * Constructor
	 *
	 * @param prop
	 * @throws IOException
	 * @throws TransformerConfigurationException
	 */
	protected GeonetXslt(Properties prop) throws IOException {
		super(prop);
		
		DocumentFactory factory = DocumentFactory.getInstance();
		factory.setXPathNamespaceURIs(NS);
		sax = new SAXReader();
		sax.setDocumentFactory(factory);
		
		TransformerFactory tfFactory = TransformerFactory.newInstance();
		tfFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		tfFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

		Source xslt;

		String fname = PKG_PREFIX + "/geodcat.xslt";
		try(InputStream is = GeonetXslt.class.getResourceAsStream(fname)) {
			xslt = new StreamSource(is);
			transformer = tfFactory.newTransformer(xslt);
		} catch (TransformerConfigurationException tex) {
			throw new IOException(tex);
		}
	}
}
