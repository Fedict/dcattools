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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;

/**
 *
 * @author Bart Hanssens
 */
public class Dataset extends DataResource {

	private List<Distribution> distributions;

	/**
	 * @return the distributions
	 */
	@Override
	public List<Distribution> getDistributions() {
		return distributions;
	}

	/**
	 * @param distributions the distributions to set
	 */
	public void setDistributions(List<Distribution> distributions) {
		this.distributions = distributions;
	}

	/**
	 * Get file formats
	 * 
	 * @return set of IRIs
	 */
	@Override
	public Set<IRI> getFormats() {
		Set<IRI> formats = new HashSet<>();
		
		for(Distribution dist: distributions) {
			formats.add(dist.getFormat());
		}
		formats.remove(null);
		return formats;
	}

	/**
	 * Get licenses
	 * 
	 * @return set of IRIs
	 */
	@Override
	public Set<IRI> getLicenses() {
		Set<IRI> licenses = new HashSet<>();
		
		licenses.add(this.getLicense());
		for(Distribution dist: distributions) {
			licenses.add(dist.getLicense());
		}
		licenses.remove(null);
		return licenses;
	}

	/**
	 * Get rights
	 * 
	 * @return set of IRIs
	 */
	public Set<IRI> getRights() {
		Set<IRI> rights = new HashSet<>();
		
		for(Distribution dist: distributions) {
			rights.add(dist.getRight());
		}
		rights.remove(null);
		return rights;
	}

	/**
	 * Get access URLS for a specific language
	 * 
	 * @param lang language code
	 * @return set of IRIs
	 */
	public Set<IRI> getAccesURLs(String lang) {
		Set<IRI> urls = new HashSet<>();
		
		for(Distribution dist: distributions) {
			Set<IRI> s = dist.getAccessURLs(lang);
			if (s != null) {
				urls.addAll(s);
			}
		}
		return urls;
	}

	/**
	 * Get download URLS for a specific language
	 * 
	 * @param lang language code
	 * @return set of IRIs
	 */
	@Override
	public Set<IRI> getDownloadURLs(String lang) {
		Set<IRI> urls = new HashSet<>();
		
		for(Distribution dist: distributions) {
			Set<IRI> s = dist.getDownloadURLs(lang);
			if (s != null) {
				urls.addAll(s);
			}
		}
		return urls;
	}

}
