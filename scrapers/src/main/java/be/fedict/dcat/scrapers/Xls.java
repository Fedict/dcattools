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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract scraper for Excel files.
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public abstract class Xls extends Scraper {
	/**
	 * Convert an Excel numeric ID to a string
	 *
	 * @param s value
	 * @return string value
	 */
	protected static String stringInt(String s) {
		if (!s.isEmpty() && s.endsWith(".0")) {
			return (s.substring(0, s.length() - 2));
		}
		return s;
	}

	/**
	 * Get the column names as a list
	 *
	 * @param sheet
	 * @return list of column names
	 */
	public List<String> getColumnNames(Sheet sheet) {
		ArrayList<String> headers = new ArrayList<>();

		Row row = sheet.getRow(sheet.getFirstRowNum());
		Iterator<Cell> cells = row.cellIterator();
		while (cells.hasNext()) {
			Cell cell = cells.next();
			String header = cell.getStringCellValue().toLowerCase();
			logger.debug("Header " + header);
			headers.add(header);
		}
		return headers;
	}

	/**
	 * Parse a cell string and store it in the RDF store.
	 *
	 * @param store RDF store
	 * @param uri RDF subject URI
	 * @param map key/value object
	 * @param field CKAN field name
	 * @param property RDF property
	 * @param lang language
	 * @throws RepositoryException
	 */
	protected void parseString(Storage store, IRI uri, Map<String, String> map,
			String field, IRI property, String lang) throws RepositoryException {
		String s = map.get(field);
		if (!s.isEmpty()) {
			if (lang != null) {
				store.add(uri, property, s, lang);
			} else {
				store.add(uri, property, s);
			}
		}
	}

	/**
	 * Return the number of rows, minus the header
	 *
	 * @param sheet spreadsheet tab
	 * @return number of rows - 1
	 */
	public int getRowCount(Sheet sheet) {
		return sheet.getLastRowNum() - sheet.getFirstRowNum();
	}

	/**
	 * Get unique ID
	 *
	 * @param row
	 * @return URL
	 * @throws MalformedURLException
	 */
	protected abstract URL getId(Row row) throws MalformedURLException;

	/**
	 * Scrape rows in first sheet and store them as key/value in the cache.
	 *
	 * @param sheet spreadsheet tab
	 * @return list of URLs
	 * @throws MalformedURLException
	 */
	protected List<URL> scrapeRows(Sheet sheet) throws MalformedURLException {
		List<URL> urls = new ArrayList<>();
		Cache cache = getCache();

		List<String> names = getColumnNames(sheet);
		Iterator<Row> rows = sheet.rowIterator();
		// Skip header
		if (rows.hasNext()) {
			rows.next();
		}

		int i = 0;
		while (rows.hasNext()) {
			logger.debug("Scraping row {}", ++i);
			Row row = rows.next();
			Map<String, String> map = new HashMap<>();
			URL id = getId(row);
			// Store the cells in a key/value way
			Iterator<Cell> cells = row.cellIterator();
			while (cells.hasNext()) {
				Cell cell = cells.next();
				String key = names.get(cell.getColumnIndex());
				map.put(key, cell.toString());
			}
			cache.storeMap(id, map);
			urls.add(id);
		}
		return urls;
	}

	/**
	 * Generate DCAT dataset
	 *
	 * @param store
	 * @param map
	 * @throws RepositoryException
	 * @throws MalformedURLException
	 */
	public abstract void generateDataset(Storage store, Map<String, String> map, URL u)
			throws RepositoryException, MalformedURLException;

	@Override
	public void generateDcat(Cache cache, Storage store)
			throws RepositoryException, MalformedURLException {
		logger.info("Generate DCAT");

		/* Get the list of all datasets */
		List<URL> urls = cache.retrieveURLList();
		for (URL u : urls) {
			Map<String, String> map = cache.retrieveMap(u);
			generateDataset(store, map, u);
		}
		generateCatalog(store);
	}

	/**
	 * Scrape workbook
	 *
	 * @return
	 * @throws IOException
	 */
	protected List<URL> scrapeWorkbook() throws IOException {
		Workbook wb = null;

		try {
			File f = new File(getBase().toURI());
			wb = WorkbookFactory.create(f);
		} catch (URISyntaxException | InvalidFormatException ex) {
			logger.error("Could not load file {}", getBase());
			throw new IOException(ex);
		}
		Sheet sheet = wb.getSheetAt(0);
		return scrapeRows(sheet);
	}

	@Override
	public void scrape() throws IOException {
		logger.info("Start scraping");
		Cache cache = getCache();

		List<URL> urls = cache.retrieveURLList();
		if (urls.isEmpty()) {
			urls = scrapeWorkbook();
			cache.storeURLList(urls);
		}

		logger.info("Found {} rows", urls.size());

		logger.info("Done scraping");
	}

	/**
	 * Constructor
	 *
	 * @param caching
	 * @param storage SDB file to be used as triple store backend
	 * @param base path to file
	 */
	public Xls(File caching, File storage, URL base) {
		super(caching, storage, base);
	}
}
