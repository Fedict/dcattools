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
package be.gov.data.dcatlib.model;

import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;

/**
 * Simplified DCAT Distribution helper class
 * 
 * @author Bart Hanssens
 */
public class Distribution {
	private IRI iri;
	private Map<String,String> title;
	private Map<String,Set<IRI>> accessURLs;
	private Map<String,Set<IRI>> downloadURLs;
	private IRI format;
	private IRI license;
	private IRI right;

	public IRI getIRI() {
		return this.iri;
	}

	public void setIRI(IRI iri) {
		this.iri = iri;
	}

	/**
	 * @return the title
	 */
	public Map<String,String> getTitle() {
		return title;
	}

	/**
	 * @param lang
	 * @return the title
	 */

	public String getTitle(String lang) {
		return title.get(lang);
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
	 * @return the accessURLs
	 */
	public Map<String,Set<IRI>> getAccessURLs() {
		return accessURLs;
	}

	/**
	 * @param lang
	 * @return the accessURLs
	 */
	public Set<IRI> getAccessURLs(String lang) {
		return lang.equals("") ? accessURLs.get(lang)
								: accessURLs.getOrDefault(lang, getAccessURLs(""));
	}

	/**
	 * Get access URLs for a specific language
	 * 
	 * @param accessURLs the accessURL to set
	 */
	public void setAccessURLs(Map<String,Set<IRI>> accessURLs) {
		this.accessURLs = accessURLs;
	}

	/**
	 * @return the downloadURL
	 */
	public Map<String,Set<IRI>> getDownloadURLs() {
		return downloadURLs;
	}
	
	/**
	 * Get download URL for a specific language
	 * 
	 * @param lang
	 * @return the downloadURL
	 */
	public Set<IRI> getDownloadURLs(String lang) {
		return lang.equals("") ? downloadURLs.get(lang)
								: downloadURLs.getOrDefault(lang, getDownloadURLs(""));
	}

	/**
	 * @param downloadURLs the downloadURL to set
	 */
	public void setDownloadURLs(Map<String,Set<IRI>> downloadURLs) {
		this.downloadURLs = downloadURLs;
	}

	/**
	 * @return the license
	 */
	public IRI getLicense() {
		return license;
	}

	/**
	 * @param license the license to set
	 */
	public void setLicense(IRI license) {
		this.license = license;
	}
	
	/**
	 * @return the right
	 */
	public IRI getRight() {
		return right;
	}

	/**
	 * @param right the right to set
	 */
	public void setRight(IRI right) {
		this.right = right;
	}
}
