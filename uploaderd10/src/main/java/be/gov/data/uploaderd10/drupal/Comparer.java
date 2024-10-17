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
package be.gov.data.uploaderd10.drupal;

import be.gov.data.dcatlib.DcatReader;
import be.gov.data.dcatlib.model.Catalog;
import be.gov.data.dcatlib.model.DataResource;
import be.gov.data.dcatlib.model.Distribution;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.eclipse.rdf4j.model.IRI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compare datasets from an input file with the datasets (for that user) on the data.gov.be portal
 * 
 * @author Bart Hanssens
 */
public class Comparer {
	private static final Logger LOG = LoggerFactory.getLogger(Comparer.class);

	private final DrupalClient client;
	private final DcatReader reader;
	private final Hasher hasher;

	private Map<String,Integer> categories;
	private Map<String,Integer> licenses;
	private Map<String,Integer> ftypes;
	private Map<String,Integer> frequencies;
	private Map<String,Integer> geos;
	private Map<String,Integer> organisations;

	/**
	 * Load Drupal taxonomies for mapping purposes
	 * 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private void getTaxonomies() throws IOException, InterruptedException {
		categories = client.getTaxonomy("category");
		licenses = client.getTaxonomy("license");
		ftypes = client.getTaxonomy("file_type");
		frequencies = client.getTaxonomy("frequency");
		geos = client.getTaxonomy("geo_coverage");
		organisations = client.getTaxonomy("organisation");
	}

	/**
	 * Map IRI to Drupal taxonomy value
	 * 
	 * @param taxonomy
	 * @param iri
	 * @return 
	 */
	private Integer mapTaxonomy(Map<String,Integer> taxonomy, IRI iri) {
		if (iri == null) {
			return null;
		}
		Integer value = taxonomy.get(iri.stringValue());
		if (value == null) {
			LOG.error("No Drupal value for IRI '{}'", iri);
		}
		return value;
	}

	/**
	 * Map IRI to Drupal taxonomy values
	 * 
	 * @param taxonomy
	 * @param iris
	 * @return 
	 */
	private Set<Integer> mapTaxonomy(Map<String,Integer> taxonomy, Set<IRI> iris) {
		Set<Integer> s = new HashSet<>();
		if (iris == null || iris.isEmpty()) {
			return s;
		}
		for (IRI iri: iris) {
			Integer value = mapTaxonomy(taxonomy, iri);
			if (value != null) {
				s.add(value);
			}
		}
		return s;
	}

	/**
	 * Get first non-NULL IRI
	 * 
	 * @param iris
	 * @return 
	 */
	private IRI getFirst(IRI... iris) {
		for(IRI iri: iris) {
			if (iri != null) {
				return iri;
			}
		}
		return null;
	}


	/**
	 * Convert a set of (RDF) IRIs to (Java) URIs
	 * 
	 * @param iris set of RDF IRIs
	 * @return set of Java URI
	 */
	private Set<URI> toURI(Set<IRI> iris) {
		if (iris == null) {
			return null;
		}
		Set<URI> uris = new HashSet<>();
		for (IRI iri: iris) {	
			try {
				uris.add(new URI(iri.stringValue()));
			} catch (URISyntaxException ex) {
				LOG.error("Could not convert {} to URI", iri.stringValue());
			}
		}
		return uris;
	}

	/**
	 * Convert a set of (RDF) IRIs to a map of (Java) URIs with a string value (useful for titles in hyperlinks)
	 * For now just use the last part of the URI as title.
	 * 
	 * @param iris
	 * @return map of URI with their titles
	 */
	private Map<URI, String> toURIMap(Set<IRI> iris) {
		Set<URI> uris = toURI(iris);
		if (uris == null) {
			return null;
		}
		return uris.stream()
					.collect(Collectors.toMap(u -> u, 
										u -> StringUtils.substringAfterLast(u.getPath(), "/")));
	}

	/**
	 * Remove mailto: from email address
	 * 
	 * @param mail mail IRI
	 * @return email address without mailto
	 */
	private Set<String> stripMailto(IRI mail) {
		if (mail == null) {
			return null;
		}
		String s = mail.stringValue();
		return Set.of(s.startsWith("mailto:") ? s.substring(7) : s);
	}

	/**
	 * Map a DCAT dataset to a Drupal dataset
	 * 
	 * @param datasets
	 * @param lang
	 * @return 
	 */
	private Map<ByteBuffer, DrupalDataset> mapToDrupal(Map<String, DataResource> datasets, String lang) {
		Map<ByteBuffer, DrupalDataset> map = new HashMap<>();
		
		for(DataResource d: datasets.values()) {
			DrupalDataset drupal = new DrupalDataset(
				null,
				null,
				d.getId(),
				d.getTitle(lang),
				d.getDescription(lang),
				lang,
				mapTaxonomy(categories, d.getThemes()),
				null,
				stripMailto(d.getContactAddr(lang)),
				toURI(Set.of(d.getLandingPage(lang))),
				toURIMap(d.getDownloadURLs(lang)),
				d.getKeywords(lang),
				mapTaxonomy(ftypes, d.getFormats()),
				mapTaxonomy(frequencies, d.getAccrualPeriodicity()),
				mapTaxonomy(geos, d.getSpatial()),
				mapTaxonomy(licenses, d.getLicenses().stream().findFirst().orElse(null)),
				d.getContactName(lang),
				mapTaxonomy(organisations, d.getPublisher()),
				d.getCreators(lang),
				d.getStartDate(),
				d.getEndDate(),
				d.getModified(),
				d.isHvd(),
				toURI(d.getLegislation())
			);
			map.put(ByteBuffer.wrap(hasher.hash(drupal)), drupal);
		}

		return map;
	}

	/**
	 * Create missing datasets on Drupal using the ones read from input file.
	 * Add the nodeIDs of newly created Drupal nodes to the input map
	 * 
	 * @param onFile datasets on file
	 * @param nodeIDs Drupal nodeIDs
	 */
	private void create(Map<String,DrupalDataset> onFile,
						Map<ByteBuffer, DrupalDataset> onSiteByHash, 
						Map<String,Integer> nodeIDs) {
		Set<String> added = new HashSet<>(onFile.keySet());
		added.removeAll(nodeIDs.keySet());
	
		LOG.info("{} to be added", added.size());

		int count = 0;
		for (String s: added) {
			try {
				DrupalDataset d = onFile.get(s);
				Integer nodeID = client.createDataset(d);
				if (nodeID < 0 || nodeID == null) {
					throw new IOException("Failed to process create response");
				}
				nodeIDs.put(s, nodeID);
				onSiteByHash.put(ByteBuffer.wrap(hasher.hash(d)), d);
				if (++count % 50 == 0) {
					LOG.info("Added {}", count);
					LOG.debug(d.toMap().toString());
				}
			} catch (IOException|InterruptedException ex) {
				LOG.error("Failed to add {} : {}", s, ex.getMessage());
			}
		}
		if (count > 0) {
			LOG.info("Added {}", count);
		}
	}

	/**
	 * Update or translated existing datasets (datasets with same ID on file as on the website)
	 * 
	 * @param onFile
	 * @param onSite
	 * @param lang language code
	 */
	private void update(Map<ByteBuffer, DrupalDataset> onFileByHash, 
						Map<ByteBuffer, DrupalDataset> onSiteByHash, 
						Map<String,Integer> nodeIDs, String lang) {
		Set<ByteBuffer> same = new HashSet<>(onFileByHash.keySet());
		same.retainAll(onSiteByHash.keySet());
		
		int nrChanged = onFileByHash.size() - same.size();
	
		LOG.info("{} to be updated / translated {}", nrChanged , lang);
		
		int count = 0;
		for (Map.Entry<ByteBuffer, DrupalDataset> d: onFileByHash.entrySet()) {
			if (! same.contains(d.getKey())) {
				try {
					Integer nid = nodeIDs.get(d.getValue().id());
					if (nid == null) {
						throw new IOException("NodeID not found");
					}
					/*	onSiteByHash.entrySet().forEach(s -> {
						if(s.getValue().id().equals(d.getValue().id())) {
							System.err.println(s.getValue());
						}});
			*/		client.updateDataset(nid, d.getValue(), lang);
					if (++count % 50 == 0) {
						LOG.info("Updated / translated {}", count);
					}
				} catch (IOException|InterruptedException ex) {
					LOG.error("Failed to update / translate {} : {}", d.getValue().id(), ex.getMessage());
				}
			}
		}
		if (count > 0) {
			LOG.info("Updated / translated {}", count);
		}
	}

	/**
	 * Delete datasets from the website that are not present (anymore) in the file
	 * 
	 * @param onFile
	 * @param nodeIDs 
	 */
	private void delete(Map<String,DrupalDataset> onFile, Map<String,Integer> nodeIDs) {
		Set<String> removed = new HashSet<>(nodeIDs.keySet());
		removed.removeAll(onFile.keySet());
	
		LOG.info("{} to be deleted", removed.size());

		int count = 0;
		for (String s: removed) {
			Integer nid = nodeIDs.get(s);
			try {
				client.deleteDataset(nid);
				nodeIDs.remove(s);
				if (++count % 50 == 0) {
					LOG.info("Deleted {}", count);
				}
			} catch (IOException|InterruptedException ex) {
				LOG.error("Failed to delete {} : {}", nid, ex.getMessage());
			}
		}
		if (count > 0) {
			LOG.info("Deleted {}", count);
		}
	}

	/**
	 * Remove incomplete (i.e. datasets with a title not available in all languages)
	 * 
	 * @param datasets
	 * @param langs 
	 */
	private void removeIncomplete(Map<String, DataResource> datasets, String[] langs) {
		int nrlangs = langs.length;
	
		Set<String> remove = new HashSet<>();

		for(Map.Entry<String,DataResource> e: datasets.entrySet()) {
			DataResource d = e.getValue();
			String key = e.getKey();
			if (d.getTitle().size() < nrlangs) {
				LOG.warn("Title for {} not available in {} languages, skipping", key, nrlangs);
				remove.add(key);
			}
			if (d.getContactAddr().isEmpty()) {
				LOG.warn("Contact addr for {} not available, skipping", key);
				remove.add(key);
			}
			if (d.getContactName().isEmpty()) {
				LOG.warn("Contact name for {} not available, skipping", key);
				remove.add(key);
			}
			if (d.getLandingPage().isEmpty()) {
				LOG.warn("Landing Page for {} not available, skipping", key);
				remove.add(key);
			}
		}
		if (!remove.isEmpty()) {
			datasets.keySet().removeAll(remove);
			LOG.warn("Removed {} datasets / services", remove.size());
		}
	}

	/**
	 * Get first element from map with key as language
	 * 
	 * @param <T>
	 * @param map
	 * @return 
	 */
	private <T> T firstFromMap(Map<String,T> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByKey())
									.findFirst().get()
									.getValue();
	}
	
	/**
	 * Provide fallback for contacts, landingPages ...
	 * If the data is not available in a language, just use the data from another language.
	 * 
	 * @param datasets
	 * @param langs languages
	 */
	private void provideFallbacks(Map<String, DataResource> datasets, String[] langs) {
		Date now = Date.from(Instant.now());

		for(DataResource d: datasets.values()) {
			if (d.getStartDate() != null && d.getEndDate() == null) {
				d.setEndDate(now);
			}
			
			for (String lang: langs) {
				if (d.getContactAddr(lang) == null) {
					Map<String, IRI> m = d.getContactAddr();
					m.put(lang, firstFromMap(m));
					LOG.debug("Adding fallback {} contact address for {}", lang, d.getId());
				}
				if (d.getContactName(lang) == null) {
					Map<String, String> m = d.getContactName();
					m.put(lang, firstFromMap(m));
					LOG.debug("Adding fallback {} contact name for {}", lang, d.getId());
				}
				if  (d.getLandingPage(lang) == null) {
					Map<String, IRI> m = d.getLandingPage();
					m.put(lang, firstFromMap(m));
					LOG.debug("Adding fallback {} landing page for {}", lang, d.getId());
				}
				if (d.getCreators(lang) == null) {
					Map<String,Set<String>> s = d.getCreators();
					if (s != null && ! s.isEmpty()) {
						s.put(lang, firstFromMap(s));
						LOG.debug("Adding fallback {} creators for {}", lang, d.getId());
					}
				}
			}

			for (Distribution dist: d.getDistributions()) {
				if (dist.getDownloadURLs() == null || dist.getDownloadURLs().isEmpty()) {
					LOG.warn("No download URLs for {} {}", dist.getIRI());
				} else {
					for (String lang: langs) {
						if (dist.getDownloadURLs(lang) == null || dist.getDownloadURLs(lang).isEmpty()) {
							Map<String, Set<IRI>> m = dist.getDownloadURLs();
							m.put(lang, firstFromMap(dist.getDownloadURLs()));
							LOG.debug("Adding fallback {} download URLS", lang);
						}
					}
				}
			}
		}
	}

	/**
	 * Read datasets / datservices from file and provide some fallbacks
	 * 
	 * @param langs
	 * @return
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private Map<String, DataResource> prepare(String[] langs) throws IOException, InterruptedException {
		getTaxonomies();

		Catalog catalog = reader.read();
		Map<String, DataResource> resources = new HashMap<>();
		resources.putAll(catalog.getDatasets());
		resources.putAll(catalog.getDataservices());

		if (resources.isEmpty()) {
			throw new IOException("No valid datasets or dataservices");
		}
		LOG.info("Read {}", resources.size());
	
		removeIncomplete(resources, langs);
		provideFallbacks(resources, langs);

		return resources;
	}
	
	/**
	 * Insert/update datasets on Drupal website (and delete datasets no longer present).
	 *
	 * To prevent accidental deletes, the sync will do nothing when the file is empty,
	 * or when the number of datasets in the file is smaller than the current size times the threshold percentage 
	 * 
	 * @param langs language codes
	 * @param threshold percent
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void sync(String[] langs, int threshold) throws IOException, InterruptedException {
		Map<String, DataResource> resources = prepare(langs);

		// Mapping of dataset / dataservice IDs to Drupal nodeIDs
		Map<String, Integer> nodeIDs = null;

		// first language is considered the "source" (always present)
		// we need this to distinguish between adding a new dataset (CREATE) or just a new translation (PATCH)
		String sourceLang = langs[0];

		for (String lang: langs) {
			Map<ByteBuffer, DrupalDataset> onFileByHash = mapToDrupal(resources, lang);
			LOG.info("Read {} {} datasets/services from file", onFileByHash.size(), lang);
			if (onFileByHash.isEmpty()) {
				throw new IOException("Zero datasets / services read, skipping");
			}

			List<DrupalDataset> ds = client.getDatasets(lang);
			LOG.info("Retrieved {} {} datasets/services from site", ds.size(), lang);
			if (onFileByHash.size() < (ds.size() * threshold / 100)) {
				throw new IOException("Number below threshold");
			}
			Set<DrupalDataset> duplicates = new HashSet<>();

			Map<ByteBuffer, DrupalDataset> onSiteByHash = ds.stream()
					.collect(Collectors.toMap(d -> ByteBuffer.wrap(hasher.hash(d)), 
											d -> d,
											(d1, d2) -> {
												LOG.error("Node {} duplicate of node {}", d1.nid(), d2.nid());
												duplicates.add(d1);
												duplicates.add(d2);
												return d1;
											} ));
			if (! duplicates.isEmpty()) {
				LOG.error("{} duplicates on site", duplicates.size());
			}
			
			Map<String,DrupalDataset> onFileById = onFileByHash.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getValue().id(), e -> e.getValue()));
			
			if (lang.equals(sourceLang)) {
				nodeIDs = ds.stream().collect(Collectors.toMap(d -> d.id(), d -> d.nid()));
				create(onFileById, onSiteByHash, nodeIDs);
			}

			update(onFileByHash, onSiteByHash, nodeIDs, lang);
	
			delete(onFileById, nodeIDs);
		}
	}
	
	/**
	 * Constructor
	 * 
	 * @param client Drupal client 
	 * @param reader dcat file reader
	 */	
	/**
	 * Constructor
	 * 
	 * @param client Drupal client 
	 * @param reader dcat file reader
	 */
	public Comparer(DrupalClient client, DcatReader reader) {
		this.client = client;
		this.reader = reader;
		this.hasher = new Hasher();
	}
}
