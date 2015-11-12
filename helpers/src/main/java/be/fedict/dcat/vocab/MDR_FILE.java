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
 * EU Publication Office Metadata Registry, File types
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class MDR_FILE {
    public static final String NAMESPACE = 
                "http://publications.europa.eu/resource/authority/file-type/";
    
    public final static String PREFIX = "mdrfile";
    
    public final static URI CSV;
    public final static URI HTML;
    public final static URI JSON;
    public final static URI KML;
    public final static URI KMZ;
    public final static URI ODS;
    public final static URI PDF;
    public final static URI RSS;
    public final static URI SHP;
    public final static URI TXT;
    public final static URI XLS;
    public final static URI XLSX;
    public final static URI XML;
    
  
    public final static Map<String,URI> MAP = new HashMap<>();
    
    static {
	ValueFactory factory = ValueFactoryImpl.getInstance();
        
        CSV = factory.createURI(MDR_FILE.NAMESPACE, "CSV");
        HTML = factory.createURI(MDR_FILE.NAMESPACE, "HTML");
        JSON = factory.createURI(MDR_FILE.NAMESPACE, "JSON");
        KML = factory.createURI(MDR_FILE.NAMESPACE, "KML");
        KMZ = factory.createURI(MDR_FILE.NAMESPACE, "KMZ");
        ODS = factory.createURI(MDR_FILE.NAMESPACE, "ODS");
        PDF = factory.createURI(MDR_FILE.NAMESPACE, "PDF");
        RSS = factory.createURI(MDR_FILE.NAMESPACE, "RSS");
        SHP = factory.createURI(MDR_FILE.NAMESPACE, "SHP");
        TXT = factory.createURI(MDR_FILE.NAMESPACE, "TXT");
        XLS = factory.createURI(MDR_FILE.NAMESPACE, "XLS");
        XLSX = factory.createURI(MDR_FILE.NAMESPACE, "XLSX");
        XML = factory.createURI(MDR_FILE.NAMESPACE, "XML");
        
        MAP.put("csv", CSV);
        MAP.put("htm", HTML);
        MAP.put("html", HTML);
        MAP.put("json", JSON);
        MAP.put("kml", KML);
        MAP.put("kmz", KMZ);
        MAP.put("ods", ODS);
        MAP.put("pdf", PDF);
        MAP.put("rss", RSS);
        MAP.put("shp", SHP);
        MAP.put("txt", TXT);
        MAP.put("xls", XLS);
        MAP.put("xlsx", XLSX);
        MAP.put("xml", XML);
    }
}
