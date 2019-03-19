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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.http.HttpHeaders;
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
     * Get delay between HTTP requests
     * 
     * @return 
     */
    public int getDelay() {
        return delay;
    }
    /**
     * Set delay between HTTP requests (500 ms or higher)
     * 
     * @param delay milliseconds
     */
    public void setDelay(int delay) {
		if (delay >= 500) {
			this.delay = delay;
			logger.info("Setting delay to {} ms", delay);
		}
		logger.warn("Delay of {} ms is too low", delay);
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
        String json = request.execute().returnContent().asString();
        JsonReader reader = Json.createReader(new StringReader(json));

		return reader.readObject();
	}

    /**
     * Make HTTP GET request, assuming UTF8 response
     * 
     * @param url
     * @return String containing raw page or empty string
     * @throws IOException 
     */
	public String makeRequest(URL url) throws IOException {
		return makeRequest(url, StandardCharsets.UTF_8);
	}
    
    /**
     * Make HTTP GET request, assuming a specific charset used in the response
     *
     * @param url
	 * @param charset response charset
     * @return String containing raw page or empty string
     * @throws IOException 
     */
    public String makeRequest(URL url, Charset charset) throws IOException {
        logger.info("Get request for page {}", url);
        Request request = Request.Get(url.toString());
		// some servers return 503 if no accept header is present
		request.addHeader(HttpHeaders.ACCEPT, "*/*");
		request.connectTimeout(110 * 1000);
		request.socketTimeout(110 * 1000);
        HttpResponse res = request.execute().returnResponse();
        // Return empty if the HTTP returns something faulty
        int status = res.getStatusLine().getStatusCode();
        if (status != 200) {
            logger.warn("HTTP code {} getting page {}", status, url);
            return "";
        }

        return EntityUtils.toString(res.getEntity(), charset);
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
        
        return request.execute().returnResponse()
                                .getStatusLine().getStatusCode();
    }
}
