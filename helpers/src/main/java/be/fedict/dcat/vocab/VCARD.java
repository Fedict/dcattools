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
public class VCARD {
    public static final String NAMESPACE = "http://www.w3.org/2006/vcard/ns#";
    
    public final static String PREFIX = "vcard";
    
    public final static IRI A_ORGANIZATION;

    public final static IRI HAS_ADDRESS;
    public final static IRI HAS_EMAIL;
    public final static IRI HAS_FN;
    public final static IRI HAS_TEL;
    
    static {
	ValueFactory factory = SimpleValueFactory.getInstance();
        A_ORGANIZATION = factory.createIRI(VCARD.NAMESPACE, "Organization");

        HAS_ADDRESS = factory.createIRI(VCARD.NAMESPACE, "hasAddress");
        HAS_EMAIL = factory.createIRI(VCARD.NAMESPACE, "hasEmail");
        HAS_FN = factory.createIRI(VCARD.NAMESPACE, "fn");
        HAS_TEL = factory.createIRI(VCARD.NAMESPACE, "hasTelephone");
    }
}
