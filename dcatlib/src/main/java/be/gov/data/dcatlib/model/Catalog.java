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
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;

/**
 *
 * @author Bart Hanssens
 */
public class Catalog {
	private final Map<String,Dataset> datasets = new HashMap<>(12_000);
	private final Map<String,Dataservice> dataservices = new HashMap<>(500);
	private final Map<String,CatalogRecord> records = new HashMap<>(12000);
	private final Map<String,SkosTerm> terms = new HashMap<>(250);
	private final Map<String,Organization> orgs = new HashMap<>(250);
	
	/**
	 * Add dataset with specific ID
	 * 
	 * @param id
	 * @param dataset
	 * @throws IOException when ID is already present (= duplicate)
	 */

	public void addDataset(String id, Dataset dataset) throws IOException {
		if (datasets.containsKey(id)) {
			throw new IOException("Key " + id + " already present");
		}
		datasets.put(id, dataset);
	}

	/**
	 * Get the dataset with the specific ID
	 * 
	 * @param id ID
	 * @return dataset or null
	 */
	public Dataset getDataset(String id) {
		return datasets.get(id);
	}

	/**
	 * Get the dataset with the specific IRI
	 * 
	 * @param iri iri
	 * @return dataset or null
	 */
	public Dataset getDataset(IRI iri) {
		for (Dataset d: datasets.values()) {
			if (d.getIRI().equals(iri)) {
				return d;
			}
		}
		return null;
	}
	
	/**
	 * Get a map of all datasets, with ID as key
	 * 
	 * @return 
	 */
	public Map<String,Dataset> getDatasets() {
		return datasets;
	}

	/**
	 * Add dataservice with specific ID
	 * 
	 * @param id
	 * @param dataservice 
	 * @throws IOException when ID is already present (= duplicate)
	 */
	public void addDataservice(String id, Dataservice dataservice) throws IOException {
		if (dataservices.containsKey(id)) {
			throw new IOException("Key " + id + " already present");
		}
		dataservices.put(id, dataservice);
	}

	/**
	 * Get the dataservice with specific ID
	 * 
	 * @param id
	 * @return dataset or null
	 */
	public Dataservice getDataservice(String id) {
		return dataservices.get(id);
	}

	/**
	 * Get a map of all dataservices, with ID as key
	 * 
	 * @return 
	 */
	public Map<String,Dataservice> getDataservices() {
		return dataservices;
	}

	/**
	 * Add term with specific ID
	 * 
	 * @param id
	 * @param term 
	 * @throws IOException when ID is already present (= duplicate)
	 */
	public void addTerm(String id, SkosTerm term) throws IOException {
		if (terms.containsKey(id)) {
			throw new IOException("Key " + id + " already present");
		}
		terms.put(id, term);
	}

	/**
	 * Get the term with specific ID
	 * 
	 * @param id
	 * @return term or null
	 */
	public SkosTerm getTerm(String id) {
		return terms.get(id);
	}

	/**
	 * Get the terms
	 * 
	 * @return 
	 */
	public Map<String,SkosTerm> getTerms() {
		return terms;
	}

	/**
	 * Add catalog record with specific ID
	 * 
	 * @param id
	 * @param record
	 * @throws IOException when ID is already present (= duplicate)
	 */
	public void addRecord(String id, CatalogRecord record) throws IOException {
		if (records.containsKey(id)) {
			throw new IOException("Key " + id + " already present");
		}
		records.put(id, record);
	}

	/**
	 * Get the catalog record with specific ID
	 * 
	 * @param id
	 * @return record or null
	 */
	public CatalogRecord getCatalogRecord(String id) {
		return records.get(id);
	}

	/**
	 * Get the catalog records
	 * 
	 * @return 
	 */
	public Map<String,CatalogRecord> getCatalogRecords() {
		return records;
	}

	/**
	 * Add organization with specific ID
	 * 
	 * @param id
	 * @param org 
	 * @throws IOException when ID is already present (= duplicate)
	 */
	public void addOrganization(String id, Organization org) throws IOException {
		if (terms.containsKey(id)) {
			throw new IOException("Key " + id + " already present");
		}
		orgs.put(id, org);
	}

	/**
	 * Get the organization with specific ID
	 * 
	 * @param id
	 * @return organization or null
	 */
	public Organization getOrganization(String id) {
		return orgs.get(id);
	}

	/**
	 * Get the organizations
	 * 
	 * @return 
	 */
	public Map<String,Organization> getOrganizations() {
		return orgs;
	}
}
