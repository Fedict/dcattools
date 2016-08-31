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
package be.fedict.dcat.vocab;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;


/**
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class DCAT {
    public static final String NAMESPACE = "http://www.w3.org/ns/dcat#";
    
    public final static String PREFIX = "dcat";
    
    public final static IRI A_CATALOG;
    public final static IRI A_DATASET;
    public final static IRI A_DISTRIBUTION;

    public final static IRI ACCESS_URL;
    public final static IRI BYTE_SIZE;
    public final static IRI CONTACT_POINT;
    public final static IRI DATASET;
    public final static IRI DISTRIBUTION;
    public final static IRI DOWNLOAD_URL;
    public final static IRI KEYWORD;
    public final static IRI LANDING_PAGE;
    public final static IRI MEDIA_TYPE;
    public final static IRI THEME;
    
    
    static {
	ValueFactory factory = SimpleValueFactory.getInstance();
        A_CATALOG = factory.createIRI(DCAT.NAMESPACE, "Catalog");
        A_DATASET = factory.createIRI(DCAT.NAMESPACE, "Dataset");
        A_DISTRIBUTION = factory.createIRI(DCAT.NAMESPACE, "Distribution");
        
        ACCESS_URL = factory.createIRI(DCAT.NAMESPACE, "accessURL");
        BYTE_SIZE = factory.createIRI(DCAT.NAMESPACE, "byteSize");
        CONTACT_POINT = factory.createIRI(DCAT.NAMESPACE, "contactPoint");
        DATASET = factory.createIRI(DCAT.NAMESPACE, "dataset");
        DISTRIBUTION = factory.createIRI(DCAT.NAMESPACE, "distribution");
        DOWNLOAD_URL = factory.createIRI(DCAT.NAMESPACE, "downloadURL");
        KEYWORD = factory.createIRI(DCAT.NAMESPACE, "keyword");
        LANDING_PAGE = factory.createIRI(DCAT.NAMESPACE, "landingPage");
        MEDIA_TYPE = factory.createIRI(DCAT.NAMESPACE, "mediaType");
        THEME = factory.createIRI(DCAT.NAMESPACE, "theme");
    }
}
