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

import be.gov.data.drupal10.Drupal;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Bart Hanssens
 */
public class TaxonomyTest {
	static Drupal d10;
	
	@BeforeAll
	public static void setUpClass() throws IOException, InterruptedException {
		Properties prop = new Properties();
		try (InputStream is = Files.newInputStream(Paths.get("test.properties"))) {
			prop.load(is);
		}
		System.err.print("site");
		System.err.print(prop.getProperty("site"));
		d10 = new Drupal(prop.getProperty("site"), prop.getProperty("auth"));
		d10.login(prop.getProperty("user"), prop.getProperty("pass"));
	}

	@Test
	public void testCategory() throws IOException, InterruptedException {
		Map taxo = d10.getTaxonomy("category");
		Assertions.assertFalse(taxo.isEmpty());
	}

	@Test
	public void testLicense() throws IOException, InterruptedException {
		Map taxo = d10.getTaxonomy("license");
		Assertions.assertFalse(taxo.isEmpty());
	}

	@Test
	public void testFormat() throws IOException, InterruptedException {
		Map taxo = d10.getTaxonomy("file_type");
		Assertions.assertFalse(taxo.isEmpty());
	}
	
	@Test
	public void testFrequency() throws IOException, InterruptedException {
		Map taxo = d10.getTaxonomy("frequency");
		Assertions.assertFalse(taxo.isEmpty());
	}

	@Test
	public void testGeo() throws IOException, InterruptedException {
		Map taxo = d10.getTaxonomy("geo_coverage");
		Assertions.assertFalse(taxo.isEmpty());
	}

	@Test
	public void testOrganisation() throws IOException, InterruptedException {
		Map taxo = d10.getTaxonomy("organisation");
		Assertions.assertFalse(taxo.isEmpty());
	}
}
