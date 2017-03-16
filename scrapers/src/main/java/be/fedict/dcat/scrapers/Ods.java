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
import be.fedict.dcat.helpers.Page;
import be.fedict.dcat.helpers.Storage;

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.eclipse.rdf4j.repository.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract OpenDataSoft scraper.
 *
 * @see https://www.opendatasoft.com/
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public abstract class Ods extends Scraper {

	private final Logger logger = LoggerFactory.getLogger(Ods.class);

	public final static String API_DCAT = "/api/datasets/1.0/search?format=rdf&rows=-1";
	public final static String API_PAGE = "/explore/dataset/";
	public final static String API_EXP = "/export/";

	/**
	 * Scrape DCAT catalog.
	 *
	 * @param cache
	 * @throws IOException
	 */
	protected abstract void scrapeCat(Cache cache) throws IOException;

	@Override
	public void scrape() throws IOException {
		logger.info("Start scraping");
		Cache cache = getCache();

		Map<String, Page> front = cache.retrievePage(getBase());
		if (front.keySet().isEmpty()) {
			scrapeCat(cache);
		}
		logger.info("Done scraping");
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
	public abstract void generateDcat(Cache cache, Storage store)
			throws RepositoryException, MalformedURLException;

	/**
	 * Constructor
	 *
	 * @param caching
	 * @param storage
	 * @param base
	 */
	public Ods(File caching, File storage, URL base) {
		super(caching, storage, base);
	}
}
