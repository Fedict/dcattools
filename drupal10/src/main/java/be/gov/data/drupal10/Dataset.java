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

import jakarta.mail.internet.InternetAddress;

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
 * Drupal 10 dataset
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
	Set<InternetAddress> contacts,
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

	private static DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
	
	/**
	 * Create a nested map containing the required values
	 * 
	 * @return map
	 */
	public Map<String,Object> toMap() {
		Map<String,Object> map = new HashMap<>();

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
	}

	private static List getList(String field, Map<String,Object> map) {
		if (map == null) {
			return null;
		}
		Object obj = map.getOrDefault(field, null);
		if (obj == null) {
			return null;
		}
		List<Map<String,Object>> lst = (List) obj;
		if (lst.isEmpty()) {
			return null;
		}
		return lst;
	}

	/**
	 * Get first value from the JSON map
	 * 
	 * @param field field name
	 * @param obj JSON object as map
	 * @param key value key
	 * @return value or null
	 */
	private static Object getOneValue(String field, Map<String,Object> map, String key) {
		List<Map<String,Object>> lst = getList(field, map);
		if (lst == null) {
			return null;
		}
		Map<String,Object> m = lst.get(0);
		return m.getOrDefault(key, null);
	}

	private static LocalDate getOneDateValue(String field, Map<String,Object> map, String key) {
		String str = (String) getOneValue(field, map, key);
		if (str == null || str.isEmpty()) {
			return null;
		}
		return LocalDate.parse(str, fmt);
	}

	private static <T> Set<T> getSet(String field, Map<String,Object> map, String key, Class<T> clazz) {
		List<Map<String,Object>> lst = getList(field, map);
		if (lst == null) {
			return null;
		}
		
		return lst.stream()
			.map(m -> m.getOrDefault(key, null))
			.filter(m -> m != null)
			.map(m -> {
					if (m instanceof Integer) {
						return clazz.cast(m);
					}
					try {
						return clazz.getConstructor(String.class).newInstance(m); 
					} catch (Exception e) {
						System.err.println(e.getMessage());
						return null;
					}
				})
			.collect(Collectors.toSet());
	}

	/**
	 * Create a dataset map containing the required values
	 * 
	 * @return map
	 */
	public static Dataset fromMap(Map<String,Object> map) {
		return new Dataset(
			(String) getOneValue("field_id", map, "value"),
			(String) getOneValue("title", map, "value"),
			(String) getOneValue("body", map, "value"),
			(String) getOneValue("langcode", map, "value"),
			getSet("field_category", map, "target_id", Integer.class),
			getSet("field_conditions", map, "uri", URI.class),
			getSet("field_contact", map, "value", InternetAddress.class),
			getSet("field_details", map, "uri", URI.class),
			getSet("field_links", map, "uri", URI.class),
			getSet("field_keywords", map, "value", String.class),
			getSet("field_file_type", map, "target_id", Integer.class),
			(Integer) getOneValue("field_frequency", map, "target_id"),
			(Integer) getOneValue("field_geo_coverage", map, "target_id"),
			(Integer) getOneValue("field_license", map, "target_id"),
			(String) getOneValue("field_organisation", map, "value"),
			(Integer) getOneValue("field_publisher", map, "target_id"),
			getOneDateValue("field_date_range", map, "value"),
			getOneDateValue("field_date_range", map, "end_value")
		);
	}
}