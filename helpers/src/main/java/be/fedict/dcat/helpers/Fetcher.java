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
package be.fedict.dcat.helpers;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class Fetcher {
    private final Logger logger = LoggerFactory.getLogger(Fetcher.class);
    
    private HttpHost proxy = null;
    private int delay = 1000;
    
    /**
     * Sleep (between HTTP requests)
     */
    public void sleep() {
        try {
            Thread.sleep(getDelay());
        } catch (InterruptedException ex) {
        }
    }
    
    /**
     * Set HTTP proxy.
     * 
     * @param proxy proxy server
     * @param port proxy port
     */
    public void setProxy(String proxy, int port) {
        if (proxy == null || proxy.isEmpty()) {
            this.proxy = null;
        } else {
            this.proxy = new HttpHost(proxy, port);
        }
    }
    
    /**
     * Get HTTP proxy
     * 
     * @return proxy or null
     */
    public HttpHost getProxy() {
        return proxy;
    }
    
    /**
     * Get delay between HTTP requests
     * 
     * @return 
     */
    public int getDelay() {
        return delay;
    }
    
    /**
     * Make HTTP GET request.
     * 
     * @param url
     * @return JsonObject containing CKAN info
     * @throws IOException 
     */
    public JsonObject makeJsonRequest(URL url) throws IOException {
        Request request = Request.Get(url.toString());
        if (getProxy() != null) {
            request = request.viaProxy(getProxy());
        }
        String json = request.execute().returnContent().asString();
        JsonReader reader = Json.createReader(new StringReader(json));
        return reader.readObject();
    }
    
    /**
     * Make HTTP GET request.
     * 
     * @param url
     * @return String containing raw page or empty string
     * @throws IOException 
     */
    public String makeRequest(URL url) throws IOException {
        logger.info("Get request for page {}", url);
        Request request = Request.Get(url.toString());

        if (getProxy() != null) {
            request = request.viaProxy(getProxy());
        }
        HttpResponse res = request.execute().returnResponse();
        // Return empty if the HTTP returns something faulty
        int status = res.getStatusLine().getStatusCode();
        if (status != 200) {
            logger.warn("HTTP code {} getting page {}", status, url);
            return "";
        }
        return EntityUtils.toString(res.getEntity());
    }
    
    /**
     * Make HTTP HEAD request
     * 
     * @param url
     * @return
     * @throws IOException 
     */
    public int makeHeadRequest(URL url) throws IOException {
        logger.info("Head request for {}", url);
        Request request = Request.Head(url.toString());
        if (getProxy() != null) {
            request = request.viaProxy(getProxy());
        }
        return request.execute().returnResponse()
                                .getStatusLine().getStatusCode();
    }
}
