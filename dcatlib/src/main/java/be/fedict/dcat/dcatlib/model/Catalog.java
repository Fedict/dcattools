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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Bart Hanssens
 */
public class Catalog {
	private final Map<String,Dataset> datasets = new HashMap<>(10_000);
	private final Map<String,Dataservice> dataservices = new HashMap<>(500);
	
	public void addDataset(String id, Dataset dataset) throws IOException {
		if (datasets.containsKey(id)) {
			throw new IOException("Key " + id + " already present");
		}
		datasets.put(id, dataset);
	}
	
	public Dataset getDataset(String id) {
		return datasets.get(id);
	}

	public Map<String,Dataset> getDatasets() {
		return datasets;
	}

	public void addDataservice(String id, Dataservice dataservice) {
		dataservices.put(id, dataservice);
	}
	
	public Dataservice getDataservice(String id) {
		return dataservices.get(id);
	}

	public Map<String,Dataservice> getDataservices() {
		return dataservices;
	}

}
