/*
 * Copyright (c) 2015FPS BOSA DG DT
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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;



/**
 * EU Publication Office Metadata Registry, Data themes
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class MDR_THEME {
    public static final String NAMESPACE = 
                "http://publications.europa.eu/resource/authority/data-theme/";
    
    public final static String PREFIX = "mdrtheme";
    
    public final static IRI AGRI;
    public final static IRI ECON;
    public final static IRI EDUC;
    public final static IRI ENER;
    public final static IRI ENVI;
    public final static IRI GOVE;
    public final static IRI HEAL;
    public final static IRI INTR;
    public final static IRI JUST;
    public final static IRI REGI;
    public final static IRI SOCI;
    public final static IRI TECH;
    public final static IRI TRANS;
  
    public final static Map<String,IRI> MAP = new HashMap<>();
    
    static {
	ValueFactory factory = SimpleValueFactory.getInstance();
        
        AGRI = factory.createIRI(MDR_THEME.NAMESPACE, "AGRI");
        ECON = factory.createIRI(MDR_THEME.NAMESPACE, "ECON");
        EDUC = factory.createIRI(MDR_THEME.NAMESPACE, "EDUC");
        ENER = factory.createIRI(MDR_THEME.NAMESPACE, "ENER");
        ENVI = factory.createIRI(MDR_THEME.NAMESPACE, "ENVI");
        GOVE = factory.createIRI(MDR_THEME.NAMESPACE, "GOVE");
        HEAL = factory.createIRI(MDR_THEME.NAMESPACE, "HEAL");
        INTR = factory.createIRI(MDR_THEME.NAMESPACE, "INTR");
        JUST = factory.createIRI(MDR_THEME.NAMESPACE, "JUST");
        REGI = factory.createIRI(MDR_THEME.NAMESPACE, "REGI");
        SOCI = factory.createIRI(MDR_THEME.NAMESPACE, "SOCI");
        TECH = factory.createIRI(MDR_THEME.NAMESPACE, "TECH");
        TRANS = factory.createIRI(MDR_THEME.NAMESPACE, "TRANS");
    }
}
