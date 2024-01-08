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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
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

	private String stripMailto(String s) {
		if (s == null) {
			return null;
		}
		return s.startsWith("mailto:") ? s.substring(7) : s;
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
				Set.of(),
				Set.of(stripMailto(d.getContactAddr(lang).stringValue())),
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
	 * Remove datasets that have not been changed (based on hash) from both file map and site map
	 * 
	 * @param onFile
	 * @param onSite 
	 */
	private void removeUnchanged(Map<ByteBuffer,DrupalDataset> onFile, Map<ByteBuffer,DrupalDataset> onSite) {
		Set<ByteBuffer> same = new HashSet<>(onSite.keySet());
		same.retainAll(onFile.keySet());

		onFile.keySet().removeAll(same);
		LOG.info("{} on file but not on site", onFile.size());

		onSite.keySet().removeAll(same);
		LOG.info("{} on site but not on file", onSite.size());
	}

	/**
	 * Update existing datasets (based on dcterms identifier)
	 * 
	 * @param onFile
	 * @param onSite
	 * @param lang 
	 */
	private void update(Map<String,DrupalDataset> onFile, Map<String,Integer> nodeIDs, String lang) {
		Set<String> same = new HashSet<>(nodeIDs.keySet());
		same.retainAll(onFile.keySet());
	
		LOG.info("{} to be updated", same.size());
		
		int count = 0;
		for (DrupalDataset d: onFile.values()) {
			if (same.contains(d.id())) {
				count++;
				Integer nid = nodeIDs.get(d.id());
				try {
					client.updateDataset(nid, d, lang);
					if (count % 100 == 0) {
						LOG.info("Updated {}", count);
					}
				} catch (IOException|InterruptedException ex) {
					LOG.error("Failed to update {} ({}) : {}", nid, d.title(), ex.getMessage());
				}
			}
		}
		LOG.info("Updated {}", count);

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

		// Drupal nodeIDs
		Map<String, Integer> nodeIDs = null;
		
		for (String lang: langs) {
			Map<ByteBuffer, DrupalDataset> onFileByHash = mapToDrupal(datasets, lang);
			LOG.info("Read {} {} datasets from file", onFileByHash.size(), lang);

			List<DrupalDataset> l = client.getDatasets(lang);
			Map<ByteBuffer, DrupalDataset> onSiteByHash = l.stream()
					.collect(Collectors.toMap(d -> ByteBuffer.wrap(hasher.hash(d)), d -> d));
			LOG.info("Retrieved {} {} datasets from site", onSiteByHash.size(), lang);
	
			removeUnchanged(onFileByHash, onSiteByHash);
			
			// first language is considered the "source" (always present)
			// we need this to distinguish between adding a new dataset (CREATE) or just a new translation (PATCH)
			if (nodeIDs == null) {
				nodeIDs = onSiteByHash.entrySet().stream()
								.collect(Collectors.toMap(e -> e.getValue().id(), e -> e.getValue().nid()));
			}
			Map<String,DrupalDataset> onFileById = onFileByHash.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getValue().id(), e -> e.getValue()));
			
			update(onFileById, nodeIDs, lang);
			
		//	Map<String,DrupalDataset> toCreate = onFileById.keySet().removeAll(onSiteById.keySet());
		//	Map<String,DrupalDataset> toDelete = onSiteById.keySet().removeAll(onFileById.keySet());
			
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
