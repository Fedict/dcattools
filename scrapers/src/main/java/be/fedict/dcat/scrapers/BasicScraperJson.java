/*
 * Copyright (c) 2021, FPS BOSA DG DT
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
package be.fedict.dcat.scrapers;

import be.fedict.dcat.helpers.Storage;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import net.minidev.json.JSONArray;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;


/**
 * Basic JSON scraper
 * 
 * @author Bart Hanssens */
public abstract class BasicScraperJson extends BaseScraper {
	protected final static Configuration conf = Configuration.builder()
												.options(Option.SUPPRESS_EXCEPTIONS)
												.options(Option.DEFAULT_PATH_LEAF_TO_NULL).build();
	
	protected static ReadContext parse(String str) {
		return JsonPath.using(conf).parse(str);
	}

	/**
	 * Add property values to model
	 * 
	 * @param store
	 * @param subj subject IRI
	 * @param jsonObj json object
	 * @param propMap property map
	 */
	protected void add(Storage store, IRI subj, ReadContext jsonObj, Map<IRI,Object> propMap) {
		propMap.forEach((k,v) -> { 
			Object obj = jsonObj.read((JsonPath) v);
			if (obj == null) {
				logger.warn("Null value for path " + ((JsonPath)v).getPath());
			} else if (obj instanceof String) {
				add(store, subj, k, obj);
			} else if (obj instanceof Integer) {
				add(store, subj, k, String.valueOf(obj));
			} else if (obj instanceof JSONArray) {
				((JSONArray) obj).forEach(p -> add(store, subj, k, p));
			} else {
				logger.warn("Unknown instance " + obj.getClass());
			}
		});
	}

	/**
	 * Add statement
	 * 
	 * @param store
	 * @param subj
	 * @param prop
	 * @param obj 
	 */
	protected void add(Storage store, IRI subj, IRI prop, Object obj) {
		if (obj == null) {
			logger.warn("Null value for {} {}", subj, prop);
			return;
		}
		Value val;
		try {
			val = Values.iri(obj.toString());
		} catch (IllegalArgumentException iae) {
			val = Values.literal(obj.toString());
		}
		store.add(subj, prop, val);
	}

	/**
	 * Constructor
	 * 
	 * @param prop 
	 */
	protected BasicScraperJson(Properties prop) throws IOException {
		super(prop);
	}
}
