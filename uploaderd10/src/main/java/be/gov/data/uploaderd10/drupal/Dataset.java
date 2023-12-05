/*
 * Copyright (c) 2023, Bart.Hanssens
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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dataset DAO
 * 
 * @author Bart Hanssens
 */
public record Dataset(
	String id,
	String title,
	String description,
	String langcode,
	Set<Integer> categories,
	Set<URI> conditions,
	Set<String> contacts,
	Set<URI> accessURLS,
	Set<URI> downloadURLS,
	String keywords,
	Set<Integer> formats,
	Integer frequency,
	Integer geography,
	Integer license,
	String organisation,
	Integer publisher,
	LocalDate from,
	LocalDate till
	) {

	private final static byte NULL = '\0';
	
	/**
	 * Calculate hash value of dataset for comparing drupal content with content from RDF file
	 * 
	 * @return hash as byte array
	 * @throws NoSuchAlgorithmException 
	 */
	public byte[] hash() throws NoSuchAlgorithmException {
		MessageDigest dg = MessageDigest.getInstance("SHA-1");
		dg.update(title.getBytes(StandardCharsets.UTF_8));
		dg.update(description.getBytes(StandardCharsets.UTF_8));
		categories.stream().forEachOrdered(c -> dg.update(c.byteValue()));
		conditions.stream().forEachOrdered(c -> dg.update(c.toString().getBytes(StandardCharsets.UTF_8)));
		contacts.stream().forEachOrdered(c -> dg.update(c.getBytes(StandardCharsets.UTF_8)));
		accessURLS.stream().forEachOrdered(c -> dg.update(c.toString().getBytes(StandardCharsets.UTF_8)));
		downloadURLS.stream().forEachOrdered(c -> dg.update(c.toString().getBytes(StandardCharsets.UTF_8)));
		
		formats.stream().forEachOrdered(c -> dg.update(c.byteValue()));
		dg.update(frequency != null ? frequency.byteValue() : NULL);
		dg.update(geography != null ? geography.byteValue() : NULL);
		dg.update(license != null ? license.byteValue() : NULL);
		dg.update(organisation != null ? organisation.getBytes(StandardCharsets.UTF_8) : new byte[]{ NULL });
		dg.update(publisher != null ? publisher.byteValue() : NULL);
		dg.update(from != null ? from.toString().getBytes(StandardCharsets.UTF_8) : new byte[]{ NULL });
		dg.update(till != null ? till.toString().getBytes(StandardCharsets.UTF_8) : new byte[]{ NULL });
		
		return dg.digest();
	}
	
	/**
	 * Convert Dataset DAO to map
	 * 
	 * @return 
	 */
	public Map<String,Object> toMap() {
		Map<String,Object> map = new HashMap();
		DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
		
		map.put("langcode", List.of(Map.of("value", langcode)));
		map.put("type", List.of(Map.of("target_id", "dataset")));
		map.put("title", List.of(Map.of("value", title)));
		map.put("body", List.of(Map.of("value", description, "format", "flexible_html")));
		map.put("field_category", categories.stream()
									.map(c -> Map.of("target_id", c))
									.collect(Collectors.toList())); 
		map.put("field_conditions", conditions.stream()
									.map(c -> Map.of("uri", c))
									.collect(Collectors.toList()));
		map.put("field_contact", contacts.stream()
									.map(c -> Map.of("value", c.toString()))
									.collect(Collectors.toList()));
		map.put("field_date_range", List.of(Map.of("value", fmt.format(from), "end_value", till)));
		map.put("field_details", accessURLS.stream()
									.map(c -> Map.of("uri", c))
									.collect(Collectors.toList()));
		map.put("field_file_type", formats.stream()
									.map(c -> Map.of("target_id", c))
									.collect(Collectors.toList()));
		map.put("field_frequency", List.of(Map.of("target_id", frequency)));
		map.put("field_geo_coverage", List.of(Map.of("target_id", geography)));
		map.put("field_id", List.of(Map.of("value", id)));
		map.put("field_keywords", List.of(Map.of("value", "keyword1")));
		map.put("field_license", List.of(Map.of("target_id", license)));
		map.put("field_links", downloadURLS.stream()
									.map(c -> Map.of("uri", c))
									.collect(Collectors.toList()));
		map.put("field_organisation", List.of(Map.of("value", organisation)));
		map.put("field_publisher", List.of(Map.of("target_id", publisher)));
		map.put("field_upstamp", List.of(Map.of("value", Instant.now().truncatedTo(ChronoUnit.SECONDS))));		
		return map;
	};
}