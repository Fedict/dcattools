/*
 * Copyright (c) 2016, FPS BOSA DG DT
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
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class XlsFavv extends Xls {

	public final static DateFormat DATEFMT = new SimpleDateFormat("yyyy-MM-dd");

	public final static String ID = "dataset";
	public final static String TITLE = "title";
	public final static String CREATED = "issued";
	public final static String DESC = "description";
	public final static String ACCESS = "access";
	public final static String DOWNLOAD = "download";

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
	protected Date getDate(Map<String, String> map, String field) {
		Date date = null;
		String s = map.getOrDefault(field, "");
		if (!s.isEmpty()) {
			try {
				date = XlsFavv.DATEFMT.parse(s);
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
	private void generateDist(Storage store, IRI dataset, Map<String, String> map,
			String id, String lang) throws RepositoryException, MalformedURLException {

		URL u = makeDistURL(id + "/" + lang + "/csv");
		IRI dist = store.getURI(u.toString());
		logger.debug("Generating distribution {}", dist.toString());

		String access = map.getOrDefault(XlsFavv.ACCESS + "@" + lang, "");
		String download = map.getOrDefault(XlsFavv.DOWNLOAD + "@" + lang, "");

		if (!download.isEmpty()) {
			store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
			store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
			store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
			store.add(dist, DCAT.ACCESS_URL, new URL(access));
			store.add(dist, DCAT.DOWNLOAD_URL, new URL(download));
			store.add(dist, DCAT.MEDIA_TYPE, "csv");
		}
	}

	@Override
	public void generateDataset(Storage store, Map<String, String> map, URL u)
			throws RepositoryException, MalformedURLException {
		IRI dataset = store.getURI(u.toString());
		logger.debug("Generating dataset {}", dataset.toString());

		store.add(dataset, RDF.TYPE, DCAT.DATASET);
		store.add(dataset, DCTERMS.IDENTIFIER, makeHashId(u.toString()));

		String[] langs = getAllLangs();
		for (String lang : langs) {
			String id = map.get(ID);
			String title = map.getOrDefault(XlsFavv.TITLE + "@" + lang, "");
			String desc = map.getOrDefault(XlsFavv.DESC + "@" + lang, title);

			store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
			store.add(dataset, DCTERMS.TITLE, title, lang);
			store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);

			Date created = getDate(map, XlsFavv.CREATED);
			if (created != null) {
				store.add(dataset, DCTERMS.ISSUED, created);
			}

			generateDist(store, dataset, map, id, lang);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param caching
	 * @param storage
	 * @param base
	 */
	public XlsFavv(File caching, File storage, URL base) {
		super(caching, storage, base);
		setName("favv");
	}
}
