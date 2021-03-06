/*
 * Copyright (c) 2020, FPS BOSA DG DT
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
import org.dom4j.Node;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;

/**
 * Scraper for the NGI Geo.be portal
 * 
 * @author Bart Hanssens
 */
public class GeonetBMDC extends GeonetGmd {
	private final static String LANDING = "http://geonetwork.bmdc.be/geonetwork/srv/eng/catalog.search#/metadata/";
	
	@Override
	protected void generateDataset(IRI dataset, String id, Storage store, Node node) 
												throws MalformedURLException {
		super.generateDataset(dataset, id, store, node);
		store.add(dataset, DCAT.LANDING_PAGE, store.getURI(LANDING + id));
	}

	/**
	 * Geonet scraper for BMDC RBINS http://geonetwork.bmdc.be
	 * 
	 * @param caching
	 * @param storage
	 * @param base 
	 */
	public GeonetBMDC(File caching, File storage, URL base) {
		super(caching, storage, base);
		setName("bmdc");
	}
}
