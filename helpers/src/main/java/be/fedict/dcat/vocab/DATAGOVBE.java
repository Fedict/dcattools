/*
 * Copyright (c) 2015, FPS BOSA DG DT
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
package be.fedict.dcat.vocab;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 *
 * @author FPS BOSA DG DT
 */
public class DATAGOVBE {

	public static final String NAMESPACE = "http://data.gov.be#";

	public final static String PREFIX = "datagovbe";

	public final static IRI FREQ;
	public final static IRI LICENSE;
	public final static IRI MEDIA_TYPE;
	public final static IRI ORG;
	public final static IRI SPATIAL;
	public final static IRI THEME;
	public final static IRI LICENSE_CC0;
	public final static IRI LICENSE_CCBY;

	public final static String PREFIX_URI_CAT = "http://data.gov.be/catalog";
	public final static String PREFIX_URI_DATASET = "http://data.gov.be/dataset";
	public final static String PREFIX_URI_TEMPORAL = "http://data.gov.be/temporal";
	public final static String PREFIX_URI_DIST = "http://data.gov.be/dist";
	public final static String PREFIX_URI_ORG = "http://data.gov.be/org";

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();

		LICENSE = factory.createIRI(DATAGOVBE.NAMESPACE, "license");
		MEDIA_TYPE = factory.createIRI(DATAGOVBE.NAMESPACE, "mediaType");
		FREQ = factory.createIRI(DATAGOVBE.NAMESPACE, "freq");
		ORG = factory.createIRI(DATAGOVBE.NAMESPACE, "org");
		SPATIAL = factory.createIRI(DATAGOVBE.NAMESPACE, "spatial");
		THEME = factory.createIRI(DATAGOVBE.NAMESPACE, "theme");

		LICENSE_CC0 = factory.createIRI("http://creativecommons.org/publicdomain/zero/1.0/");
		LICENSE_CCBY = factory.createIRI("http://creativecommons.org/licenses/by/4.0/");
	}
}
