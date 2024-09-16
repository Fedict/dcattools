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
package be.gov.data.scrapers.brussels;

import be.gov.data.helpers.Storage;
import be.gov.data.scrapers.GeonetGmd;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;
import org.dom4j.Node;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;

/**
 * Scraper for the Data Store Brussels portal
 * 
 * @see http://datastore.brussels
 * @author Bart Hanssens
 */
public class GeonetBrussels extends GeonetGmd {
	private final static String LANDING = "http://datastore.brussels/geonetwork/srv/eng/catalog.search#/metadata/";

	@Override
	protected void generateDataset(IRI dataset, String id, Storage store, Node node) 
												throws MalformedURLException {
		super.generateDataset(dataset, id, store, node);
		store.add(dataset, DCAT.LANDING_PAGE, store.getURI(LANDING + id));
	}

	/**
	 * Constructor
	 * 
	 * @param prop
	 * @throws IOException
	 */
	public GeonetBrussels(Properties prop) throws IOException {
		super(prop);
		setName("brussels");
	}
}
