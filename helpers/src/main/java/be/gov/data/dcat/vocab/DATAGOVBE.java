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
package be.gov.data.dcat.vocab;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

/**
 * Some constants
 * 
 * @author FPS BOSA DG DT
 */
public class DATAGOVBE {

	public static final String NAMESPACE = "http://data.gov.be#";

	public final static String PREFIX = "datagovbe";

	public final static IRI LICENSE_CC0 = Values.iri("http://publications.europa.eu/resource/authority/licence/CC0");

	public final static String PREFIX_URI_CAT = "http://data.gov.be/catalog";
	public final static String PREFIX_URI_DATASET = "http://data.gov.be/.well-known/genid/dataset";
	public final static String PREFIX_URI_TEMPORAL = "http://data.gov.be/.well-known/genid/temporal";
	public final static String PREFIX_URI_DIST = "http://data.gov.be/.well-known/genid/dist";
	public final static String PREFIX_URI_GEO = "http://data.gov.be/.well-known/genid/geo";
	public final static String PREFIX_URI_ORG = "http://data.gov.be/.well-known/genid/org";
	public final static String PREFIX_URI_PERSON = "http://data.gov.be/.well-known/genid/person";
}
