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
package be.gov.data.drupal10;

import jakarta.mail.Address;

import java.net.URI;
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
 * Drupal dataset
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
	Set<Address> contacts,
	Set<URI> accessURLS,
	Set<URI> downloadURLS,
	Set<String> keywords,
	Set<Integer> formats,
	Integer frequency,
	Integer geography,
	Integer license,
	String organisation,
	Integer publisher,
	LocalDate from,
	LocalDate till
	) {
	
	/**
	 * Create a nested map containing the required values
	 * 
	 * @return map
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
		if (conditions != null && !conditions.isEmpty()) {
			map.put("field_conditions", conditions.stream()
									.map(c -> Map.of("uri", c))
									.collect(Collectors.toList()));
		}
		map.put("field_contact", contacts.stream()
									.map(c -> Map.of("value", c.toString()))
									.collect(Collectors.toList()));
		if (from != null && till != null) {
			map.put("field_date_range", List.of(Map.of("value", fmt.format(from), "end_value", till)));
		}
		map.put("field_details", accessURLS.stream()
									.map(c -> Map.of("uri", c))
									.collect(Collectors.toList()));
		if (formats != null && !formats.isEmpty()) {
			map.put("field_file_type", formats.stream()
									.map(c -> Map.of("target_id", c))
									.collect(Collectors.toList()));
		}
		if (frequency != null) {
			map.put("field_frequency", List.of(Map.of("target_id", frequency)));
		}
		if (geography != null) {
			map.put("field_geo_coverage", List.of(Map.of("target_id", geography)));
		}
		map.put("field_id", List.of(Map.of("value", id)));
		if (keywords != null && !keywords.isEmpty()) {
			map.put("field_keywords", keywords.stream()
									.map(c -> Map.of("value", c))
									.collect(Collectors.toList()));
		}
		map.put("field_license", List.of(Map.of("target_id", license)));
		if (downloadURLS != null && !downloadURLS.isEmpty()) {
			map.put("field_links", downloadURLS.stream()
									.map(c -> Map.of("uri", c))
									.collect(Collectors.toList()));
		}
		map.put("field_organisation", List.of(Map.of("value", organisation)));
		map.put("field_publisher", List.of(Map.of("target_id", publisher)));
		map.put("field_upstamp", List.of(Map.of("value", Instant.now().truncatedTo(ChronoUnit.SECONDS))));

		return map;
	};
}