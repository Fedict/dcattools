/*
 * Copyright (c) 2024, Bart.Hanssens
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart.Hanssens
 */
public class Hasher {
	private final static Logger LOG = LoggerFactory.getLogger(Hasher.class);

	private final static byte[] NULL = new byte[]{'\0'};	
	private final static SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");

	private MessageDigest dg;

	/**
	 * Get byte representation of a string (for hashing purposes)
	 * 
	 * @param s
	 * @return 
	 */
	private byte[] getBytes(String s) {
		return (s != null ? s.getBytes(StandardCharsets.UTF_8) : NULL);
	}
	
	/**
	 * Get byte representation of a date (for hashing purposes)
	 * 
	 * @param d
	 * @return 
	 */
	private byte[] getBytes(Date d) {
		return (d != null ? DATE_FMT.format(d).getBytes(StandardCharsets.UTF_8) : NULL);
	}

	/**
	 * Get byte representation of an integer (for hashing purposes)
	 * 
	 * @param s
	 * @return 
	 */
	private byte[] getBytes(Integer i) {
		return (i != null ? i.toString().getBytes(StandardCharsets.UTF_8) : NULL);
	}


	/**
	 * Get byte representation of a set of URIs (for hashing purposes)
	 * 
	 * @param uris
	 * @return 
	 */
	private byte[] getBytes(Set<URI> uris) {
		if (uris == null || uris.isEmpty()) {
			return NULL;
		}
		return uris.stream().sorted()
				.map(URI::toString)
				.collect(Collectors.joining(","))
				.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Get byte representation of a set of URIs/names (for hashing purposes)
	 * 
	 * @param uris
	 * @return 
	 */
	private byte[] getBytes(Map<URI,String> uris) {
		if (uris == null || uris.isEmpty()) {
			return NULL;
		}
		return uris.keySet().stream().sorted()
				.map(URI::toString)
				.collect(Collectors.joining(","))
				.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Get byte representation of a set of integers (for hashing purposes)
	 * 
	 * @param is
	 * @return 
	 */
	private byte[] getBytesInt(Set<Integer> is) {
		if (is == null || is.isEmpty()) {
			return NULL;
		}
		return is.stream().sorted()
				.map(c -> c.toString())
				.collect(Collectors.joining(","))
				.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Get byte representation of a set of strings (for hashing purposes)
	 * 
	 * @param s
	 * @return 
	 */
	private byte[] getBytesStr(Set<String> s) {
		if (s == null || s.isEmpty()) {
			return NULL;
		}
		return s.stream().sorted()
			.collect(Collectors.joining(","))
			.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Calculate hash value of dataset, used for comparing drupal content with content from RDF file
	 * 
	 * @param ds
	 * @return hash as byte array or null
	 */
	public byte[] hash(DrupalDataset ds) {
		dg.reset();

		dg.update(getBytes(ds.title()));
		dg.update(getBytes(ds.description()));
		dg.update(getBytesInt(ds.categories()));
		dg.update(getBytes(ds.conditions()));
		dg.update(getBytesStr(ds.contacts()));
		dg.update(getBytes(ds.accessURLS()));
		dg.update(getBytes(ds.downloadURLS()));
//		dg.update(getBytesStr(ds.keywords()));
		dg.update(getBytesInt(ds.formats()));	
		dg.update(getBytes(ds.frequency()));
		dg.update(getBytes(ds.geography()));
		dg.update(getBytes(ds.license()));
		dg.update(getBytes(ds.organisation()));
		dg.update(getBytes(ds.publisher()));
		dg.update(getBytes(ds.from()));
		dg.update(getBytes(ds.till()));
		dg.update(getBytes(ds.modified()));

		return dg.digest();
	}
	
	public Hasher() {
		try {
			dg = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException n) {
			LOG.error("SHA-1 not found: {}", n.getMessage());
		}
	}
}
