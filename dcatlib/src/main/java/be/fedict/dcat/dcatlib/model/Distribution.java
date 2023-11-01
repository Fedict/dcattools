/*
 * Copyright (c) 2023, FPS BOSA
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
package be.fedict.dcat.dcatlib.model;

import java.util.Map;
import org.eclipse.rdf4j.model.IRI;

/**
 * Simplified DCAT Distribution helper class
 * 
 * @author Bart Hanssens
 */
public class Distribution {

	private Map<String,String> title;
	private Map<String,IRI> accessURL;
	private Map<String,IRI> downloadURL;
	private IRI format;

	/**
	 * @return the title
	 */
	public Map<String,String> getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(Map<String,String> title) {
		this.title = title;
	}

	/**
	 * @return the format
	 */
	public IRI getFormat() {
		return format;
	}

	/**
	 * @param format the format to set
	 */
	public void setFormat(IRI format) {
		this.format = format;
	}

	/**
	 * @return the accessURL
	 */
	public Map<String,IRI> getAccessURL() {
		return accessURL;
	}

	/**
	 * @return the accessURL
	 */
	public IRI getAccessURL(String lang) {
		return accessURL.get(lang);
	}

	/**
	 * @param accessURL the accessURL to set
	 */
	public void setAccessURL(Map<String,IRI> accessURL) {
		this.accessURL = accessURL;
	}

	/**
	 * @return the downloadURL
	 */
	public Map<String,IRI> getDownloadURL() {
		return downloadURL;
	}
	
	/**
	 * @return the downloadURL
	 */
	public IRI getDownloadURL(String lang) {
		return downloadURL.get(lang);
	}

	/**
	 * @param downloadURL the downloadURL to set
	 */
	public void setDownloadURL(Map<String,IRI> downloadURL) {
		this.downloadURL = downloadURL;
	}

}
