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

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dataset DAO, a somewhat simplified version of a DCAT Dataset (and Distribution) used by data.gov.be
 * 
 * @author Bart Hanssens
 */
public record DrupalDataset(
	Integer nid,
	String uuid,
	String id,
	String title,
	String description,
	String langcode,
	Set<Integer> categories,
	Set<URI> conditions,
	Set<String> contacts,
	Set<URI> accessURLS,
	Set<URI> downloadURLS,
	Set<String> keywords,
	Set<Integer> formats,
	Integer frequency,
	Integer geography,
	Integer license,
	String organisation,
	Integer publisher,
	Date from,
	Date till,
	Date modified
	) {

	private final static Logger LOG = LoggerFactory.getLogger(DrupalDataset.class);

	private final static SimpleDateFormat DATE_FMT_FULL = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	private final static SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");

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

	/**
	 * Get first date or null
	 * 
	 * @param field
	 * @param map
	 * @param key
	 * @return date or null
	 */
	private static Date getOneDateValue(String field, Map<String,Object> map, String key) {
		String str = (String) getOneValue(field, map, key);
		if (str == null || str.isEmpty()) {
			return null;
		}
		try {
			return DATE_FMT.parse(str);
		} catch (ParseException ex) {
			LOG.error("Could not parse date {}", str);
			return null;
		}
	}

	/**
	 * Get a list from map
	 * 
	 * @param field
	 * @param map
	 * @return 
	 */
	private static List<Map<String,Object>> getList(String field, Map<String,Object> map) {
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
	 * Get a set of objects from a (JSON) structure
	 * 
	 * @param <T>
	 * @param field (JSON) field name to use
	 * @param map map containing the data
	 * @param key key (within JSON object) to search for
	 * @param clazz type of the objects
	 * @return a set of objects of class clazz
	 */
	private static <T> Set<T> getSet(String field, Map<String,Object> map, String key, Class<T> clazz) {
		List<Map<String,Object>> lst = getList(field, map);
		if (lst == null) {
			return null;
		}
		
		return lst.stream()
			.map(m -> m.getOrDefault(key, null))
			.filter(Objects::nonNull)
			.map(m -> {
				if (m instanceof Integer) {
					return clazz.cast(m);
				}
				try {
					return clazz.getConstructor(String.class).newInstance(m);
				} catch (Exception e) {
					LOG.error("Could not invoke contructor for field {} key {} value {}", field, key, m);
					return null;
				}
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
	}

	/**
	 * Wrap a string to a list with key-value pair
	 * 
	 * @param key
	 * @param value
	 * @return 
	 */
	private List<Map<String,String>> wrap(String key, String value) {
		// note that Map.of() does not allow null values
		Map<String,String> m = new HashMap<>();
		m.put(key, value);
		return List.of(m);
	}

	/**
	 * Wrap two values in a list of key-value pairs
	 * 
	 * @param name
	 * @param value
	 * @param name2
	 * @param value2
	 * @return 
	 */
	private List<Map<String,String>> wrap(String key, String value, String key2, String value2) {
		// note that Map.of() does not allow null values
		Map<String,String> m = new HashMap<>();
		m.put(key, value);
		m.put(key2, value2);
		return List.of(m);
	}

	/**
	 * Wrap a set of strings, limiting the length to 255 characters
	 * 
	 * @param key
	 * @param values
	 * @return 
	 */
	private Map<String,String> wrap(String key, Set<String> values) {
		Map<String,String> m = new HashMap<>();
		
		String s = null;
		
		if (values != null) {
			s = values.stream().collect(Collectors.joining(", "));
			if (s.length() > 255) {
				s = s.substring(0, 255);
				s = s.substring(0, s.lastIndexOf(","));
			}
		}
		m.put(key, s);
		return m;
	}

	/**
	 * Wrap an integer to a list with key-value pair
	 * 
	 * @param key 
	 * @param value
	 * @return 
	 */
	private List<Map<String,Integer>> wrap(String key, Integer value) {
		// note that Map.of() does not allow null values
		Map<String,Integer> m = new HashMap<>();
		m.put(key, value);
		return List.of(m);
	}

	/**
	 * Convert DrupalDataset DAO to map
	 * 
	 * @return 
	 */
	public Map<String,Object> toMap() {
		Map<String,Object> map = new HashMap<>();

		map.put("langcode", wrap("value", langcode));
		map.put("type", wrap("target_id", "dataset"));
		map.put("title", wrap("value", title.length() < 255 ? title : title.substring(0, 255)));
		map.put("body", wrap("value", description, "format", "flexible_html"));
		map.put("field_category", categories != null
									? categories.stream()
										.map(c -> Map.of("target_id", c))
										.collect(Collectors.toList())
									: null); 
		map.put("field_conditions", conditions != null
									? conditions.stream()
										.map(c -> Map.of("uri", c))
										.collect(Collectors.toList())
									: null);
		map.put("field_contact", contacts != null
									? contacts.stream()
										.map(c -> Map.of("value", c))
										.collect(Collectors.toList())
									: null);
		map.put("field_date_range", wrap("value", from != null ? DATE_FMT.format(from) : null, 
										"end_value", till != null ? DATE_FMT.format(till) : null));
		map.put("field_file_type", formats != null
									? formats.stream()
										.map(c -> Map.of("target_id", c))
										.collect(Collectors.toList())
									: null);
		map.put("field_frequency", wrap("target_id", frequency));
		map.put("field_geo_coverage", wrap("target_id", geography));
		map.put("field_id", wrap("value", id));
		map.put("field_keywords", wrap("value", keywords));
		map.put("field_license", wrap("target_id", license));
		map.put("field_details", accessURLS != null
									? accessURLS.stream()
										.map(c -> Map.of("uri", c))
										.collect(Collectors.toList())
									: null);
		map.put("field_links", downloadURLS != null
									? downloadURLS.stream()
										.map(c -> Map.of("uri", c))
										.collect(Collectors.toList())
									: null);
		map.put("field_organisation", wrap("value", organisation));
		map.put("field_publisher", wrap("target_id", publisher));
		map.put("field_upstamp", wrap("value", modified != null 
												? modified.toInstant().truncatedTo(ChronoUnit.SECONDS).toString()
												: null));
		return map;
	}

	/**
	 * Create a dataset map containing the required values
	 * 
	 * @return map
	 */
	public static DrupalDataset fromMap(Map<String,Object> map) {
		return new DrupalDataset(
			(Integer) getOneValue("nid", map, "value"),
			(String) getOneValue("uuid", map, "value"),
			(String) getOneValue("field_id", map, "value"),
			(String) getOneValue("title", map, "value"),
			(String) getOneValue("body", map, "value"),
			(String) getOneValue("langcode", map, "value"),
			getSet("field_category", map, "target_id", Integer.class),
			getSet("field_conditions", map, "uri", URI.class),
			getSet("field_contact", map, "value", String.class),
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
			getOneDateValue("field_date_range", map, "end_value"),
			getOneDateValue("field_upstamp", map, "value")
		);
	}
}