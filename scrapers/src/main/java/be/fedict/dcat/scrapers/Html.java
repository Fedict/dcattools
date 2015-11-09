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
package be.fedict.dcat.scrapers;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public abstract class Html extends Scraper {
    private final Logger logger = LoggerFactory.getLogger(Html.class);
    
    /**
     * Make a URL for a DCAT Dataset 
     * 
     * @param i
     * @return URL
     * @throws java.net.MalformedURLException 
     */
    public URL makeDatasetURL(int i) throws MalformedURLException {
        return new URL(getBase().toString() + "#" + String.valueOf(i));
    }
    
    /**
     * Make a URL for a DCAT Distribution 
     * 
     * @param i
     * @return URL
     * @throws java.net.MalformedURLException 
     */
    public URL makeDistributionURL(int i, String lang) throws MalformedURLException {
        return new URL(getBase().toString() + "#" 
                + String.valueOf(i) + "/download" + "/" + lang);
    }
       
    /**
     * HTML page scraper.
     * 
     * @param caching local cache file
     * @param storage local triple store file
     * @param base URL of the CKAN site
     */
    public Html(File caching, File storage, URL base) {
        super(caching, storage, base);
    }
}
