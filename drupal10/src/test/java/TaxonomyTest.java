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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Bart Hanssens
 */
public class TaxonomyTest {
	public static Drupal d10;
	
	@BeforeAll
	public static void init() throws InterruptedException {
		Path p = Paths.get("src","test","resources", "test.properties");
		Properties prop = new Properties();
		try (InputStream is = Files.newInputStream(p.toAbsolutePath())) {
			prop.load(is);
		} catch (IOException ioe) {
			fail();
		}

		d10 = new Drupal(prop.getProperty("site"), prop.getProperty("auth"));
		try {
			d10.login(prop.getProperty("user"), prop.getProperty("pass"));
		} catch (IOException ioe) {
			fail();
		}
	}

	@Test
	public void testCategory() throws IOException, InterruptedException {
		Map taxo = d10.getTaxonomy("category");
		System.err.println(taxo);
		Assertions.assertFalse(taxo.isEmpty());
	}

	@Test
	public void testLicense() throws IOException, InterruptedException {
		Map taxo = d10.getTaxonomy("license");
		System.err.println(taxo);
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
