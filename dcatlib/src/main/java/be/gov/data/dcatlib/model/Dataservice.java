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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;

/**
 *
 * @author Bart.Hanssens
 */
public class Dataservice extends DataResource {
	private IRI endPointURL;
	private IRI endPointDescription;
	private List<Dataset> datasets;

	/**
	 * @return the datasets
	 */
	public List<Dataset> getDatasets() {
		return datasets;
	}

	/**
	 * @param datasets the datasets to set
	 */
	public void setDatasets(List<Dataset> datasets) {
		this.datasets = datasets;
	}

	/**
	 * @return the endPointURL
	 */
	public IRI getEndPointURL() {
		return endPointURL;
	}

	/**
	 * @param endPointURL the endPointURL to set
	 */
	public void setEndPointURL(IRI endPointURL) {
		this.endPointURL = endPointURL;
	}

	/**
	 * @return the endPointDescription
	 */
	public IRI getEndPointDescription() {
		return endPointDescription;
	}

	@Override
	public Set<IRI> getDownloadURLs(String lang) {
		return (endPointURL != null) ? Set.of(endPointURL) : Collections.emptySet();
	}

	/**
	 * @param endPointDescription the endPointDescription to set
	 */
	public void setEndPointDescription(IRI endPointDescription) {
		this.endPointDescription = endPointDescription;
	}

	@Override
	public Set<IRI> getFormats() {
		Set<IRI> formats = new HashSet<>();
		
		for(Dataset d: datasets) {
			formats.addAll(d.getLicenses());
		}
		formats.remove(null);
		return formats;
	}

	@Override
	public Set<IRI> getLicenses() {
		Set<IRI> licenses = new HashSet<>();
		
		licenses.add(this.getLicense());
		for(Dataset d: datasets) {
			licenses.addAll(d.getLicenses());
		}
		licenses.remove(null);
		return licenses;
	}

	@Override
	public List<Distribution> getDistributions() {
		List<Distribution> dists = new ArrayList<>();
		for(Dataset d: datasets) {
			dists.addAll(d.getDistributions());
		}
		return dists;
	}
}
