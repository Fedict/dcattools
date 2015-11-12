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

import java.util.HashMap;
import java.util.Map;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;


/**
 * EU Publication Office Metadata Registry, Data themes
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class MDR_THEME {
    public static final String NAMESPACE = 
                "http://publications.europa.eu/resource/authority/data-theme/";
    
    public final static String PREFIX = "mdrtheme";
    
    public final static URI AGRI;
    public final static URI ECON;
    public final static URI EDUC;
    public final static URI ENER;
    public final static URI ENVI;
    public final static URI GOVE;
    public final static URI HEAL;
    public final static URI INTR;
    public final static URI JUST;
    public final static URI REGI;
    public final static URI SOCI;
    public final static URI TECH;
    public final static URI TRANS;
  
    public final static Map<String,URI> MAP = new HashMap<>();
    
    static {
	ValueFactory factory = ValueFactoryImpl.getInstance();
        
        AGRI = factory.createURI(MDR_THEME.NAMESPACE, "AGRI");
        ECON = factory.createURI(MDR_THEME.NAMESPACE, "ECON");
        EDUC = factory.createURI(MDR_THEME.NAMESPACE, "EDUC");
        ENER = factory.createURI(MDR_THEME.NAMESPACE, "ENER");
        ENVI = factory.createURI(MDR_THEME.NAMESPACE, "ENVI");
        GOVE = factory.createURI(MDR_THEME.NAMESPACE, "GOVE");
        HEAL = factory.createURI(MDR_THEME.NAMESPACE, "HEAL");
        INTR = factory.createURI(MDR_THEME.NAMESPACE, "INTR");
        JUST = factory.createURI(MDR_THEME.NAMESPACE, "JUST");
        REGI = factory.createURI(MDR_THEME.NAMESPACE, "REGI");
        SOCI = factory.createURI(MDR_THEME.NAMESPACE, "SOCI");
        TECH = factory.createURI(MDR_THEME.NAMESPACE, "TECH");
        TRANS = factory.createURI(MDR_THEME.NAMESPACE, "TRANS");
    }
}
