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

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;


/**
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class DCAT {
    public static final String NAMESPACE = "http://www.w3.org/ns/dcat#";
    
    public final static String PREFIX = "dcat";
    
    public final static URI A_CATALOG;
    public final static URI A_DATASET;
    public final static URI A_DISTRIBUTION;

    public final static URI ACCESS_URL;
    public final static URI BYTE_SIZE;
    public final static URI DATASET;
    public final static URI DISTRIBUTION;
    public final static URI DOWNLOAD_URL;
    public final static URI KEYWORD;
    public final static URI LANDING_PAGE;
    public final static URI MEDIA_TYPE;
    public final static URI THEME;
    
    
    static {
	ValueFactory factory = ValueFactoryImpl.getInstance();
        A_CATALOG = factory.createURI(DCAT.NAMESPACE, "Catalog");
        A_DATASET = factory.createURI(DCAT.NAMESPACE, "Dataset");
        A_DISTRIBUTION = factory.createURI(DCAT.NAMESPACE, "Distribution");
        
        ACCESS_URL = factory.createURI(DCAT.NAMESPACE, "accessURL");
        BYTE_SIZE = factory.createURI(DCAT.NAMESPACE, "byteSize");
        DATASET = factory.createURI(DCAT.NAMESPACE, "dataset");
        DISTRIBUTION = factory.createURI(DCAT.NAMESPACE, "distribution");
        DOWNLOAD_URL = factory.createURI(DCAT.NAMESPACE, "downloadURL");
        KEYWORD = factory.createURI(DCAT.NAMESPACE, "keyword");
        LANDING_PAGE = factory.createURI(DCAT.NAMESPACE, "landingPage");
        MEDIA_TYPE = factory.createURI(DCAT.NAMESPACE, "mediaType");
        THEME = factory.createURI(DCAT.NAMESPACE, "theme");
    }
}
