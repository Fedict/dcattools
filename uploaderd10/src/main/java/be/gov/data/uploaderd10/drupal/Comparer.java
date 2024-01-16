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
import be.gov.data.dcatlib.model.Dataset;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Hex;

import org.eclipse.rdf4j.model.IRI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compare datasets from an input file with the datasets (for that user) on the data.gov.be portal
 * 
 * @author Bart.Hanssens
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
			LOG.error("No Drupal value for IRI {}", iri.stringValue());
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
	 * @param iris
	 * @return 
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
	private Map<ByteBuffer, DrupalDataset> mapToDrupal(Map<String, Dataset> datasets, String lang) {
		Map<ByteBuffer, DrupalDataset> map = new HashMap<>();
		
		for(Dataset d: datasets.values()) {
			DrupalDataset drupal = new DrupalDataset(
				null,
				null,
				d.getId(),
				d.getTitle(lang),
				d.getDescription(lang),
				lang,
				mapTaxonomy(categories, d.getThemes()),
				toURI(d.getRights()),
				stripMailto(d.getContactAddr(lang)),
				toURI(d.getAccesURLs(lang)),
				toURI(d.getDownloadURLs(lang)),
				d.getKeywords(lang),
				mapTaxonomy(ftypes, d.getFormats()),
				mapTaxonomy(frequencies, d.getAccrualPeriodicity()),
				mapTaxonomy(geos, d.getSpatial()),
				mapTaxonomy(licenses, d.getLicenses().stream().findFirst().orElse(null)),
				d.getContactName(lang),
				mapTaxonomy(organisations, getFirst(d.getCreator(), d.getPublisher())),
				d.getStartDate(),
				d.getEndDate()
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
				if (nodeID < 0) {
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
				Integer nid = nodeIDs.get(d.getValue().id());
				try {
					client.updateDataset(nid, d.getValue(), lang);

						LOG.info( d.getValue().toString());

						onSiteByHash.entrySet()
							.forEach(e -> {
								if (e.getValue().id().equals(d.getValue().id())) {
									LOG.info(e.getValue().toString());
									LOG.info(new String(Hex.encodeHex(e.getKey())));
								}});

						onFileByHash.entrySet()
							.forEach(e -> {
								if (e.getValue().id().equals(d.getValue().id())) {
									LOG.info(e.getValue().toString());
									LOG.info(new String(Hex.encodeHex(e.getKey())));
								}});
						
	
					if (++count % 50 == 0) {
						LOG.info("Updated / translated {}", count);

					}
				} catch (IOException|InterruptedException ex) {
					LOG.error("Failed to update / translate {} ({}) : {}", nid, d.getValue().title(), ex.getMessage());
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
	private void removeIncomplete(Map<String, Dataset> datasets, String[] langs) {
		int nrlangs = langs.length;

		Iterator<Map.Entry<String, Dataset>> it = datasets.entrySet().iterator();

		while(it.hasNext()) {
			Map.Entry<String, Dataset> entry = it.next();
			if (entry.getValue().getTitle().size() < nrlangs) {
				LOG.warn("Title for {} not available in {} languages", entry.getKey(), nrlangs);
				it.remove();
			}
		}
	}

	/**
	 * Start comparing
	 * 
	 * @param langs language codes
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void compare(String[] langs) throws IOException, InterruptedException {
		getTaxonomies();

		Catalog catalog = reader.read();
		Map<String, Dataset> datasets = catalog.getDatasets();
		removeIncomplete(datasets, langs);

		// Mapping of dataset IDs to Drupal nodeIDs
		Map<String, Integer> nodeIDs = null;

		// first language is considered the "source" (always present)
		// we need this to distinguish between adding a new dataset (CREATE) or just a new translation (PATCH)
		String sourceLang = langs[0];

		for (String lang: langs) {
			Map<ByteBuffer, DrupalDataset> onFileByHash = mapToDrupal(datasets, lang);
			LOG.info("Read {} {} datasets from file", onFileByHash.size(), lang);

			List<DrupalDataset> ds = client.getDatasets(lang);
			LOG.info("Retrieved {} {} datasets from site", ds.size(), lang);

			Map<ByteBuffer, DrupalDataset> onSiteByHash = ds.stream()
					.collect(Collectors.toMap(d -> ByteBuffer.wrap(hasher.hash(d)), d -> d));

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
	 */
	public Comparer(DrupalClient client, DcatReader reader) {
		this.client = client;
		this.reader = reader;
		this.hasher = new Hasher();
	}
}
