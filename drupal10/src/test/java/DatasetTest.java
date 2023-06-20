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

import be.gov.data.drupal10.Dataset;
import be.gov.data.drupal10.Drupal;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Bart Hanssens
 */
public class DatasetTest {
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

	public void createDataset() throws AddressException, InterruptedException {
		DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
		
		Dataset d2 = new Dataset(
				"id2", 
				"Titel 2", 
				"Beschrijving 2, dit kan een lange string zijn", 
				"nl",
				Set.of(36, 37), 
				Set.of(URI.create("http://www.example.condition.nl")),
				Set.of(new InternetAddress("info@example.be")),
				Set.of(URI.create("http://www.example.access1.nl"), URI.create("http://www.example.access2.nl")),
				Set.of(URI.create("http://www.example.download1.nl"), URI.create("http://www.example.download2.nl")),
				Collections.EMPTY_SET,
				Set.of(66, 2),
				25,
				333,
				173,
				"Contact 2 org nl",
				76,
				LocalDate.parse("2000-05-01", fmt),
				LocalDate.parse("2005-10-03", fmt));

		try {
			d10.createDataset(d2);
		} catch (IOException ioe) {
			fail();
		}
	}

	public void updateDataset() throws AddressException, InterruptedException {
		DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
		
		Dataset d2 = new Dataset(
				"id2", 
				"Titel 2 updated", 
				"Beschrijving 2, dit kan een lange string zijn, updated", 
				"nl",
				Set.of(36), 
				Set.of(URI.create("http://www.example.condition.nl")),
				Set.of(new InternetAddress("info@example.updated.be")),
				Set.of(URI.create("http://www.example.access1.nl"), URI.create("http://www.example.access2.nl")),
				Set.of(URI.create("http://www.example.download1.nl"), URI.create("http://www.example.download2.nl")),
				Collections.EMPTY_SET,
				Set.of(66),
				25,
				333,
				173,
				"Contact 2 org nl updated",
				76,
				LocalDate.parse("2001-05-01", fmt),
				LocalDate.parse("2005-10-03", fmt));

		try {
			d10.updateDataset(d2, "nl");
		} catch (IOException ioe) {
			fail();
		}
	}
	
	public void addDatasetTranslation() throws AddressException, InterruptedException {
		DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
		
		Dataset d2 = new Dataset(
				"id2", 
				"Titre 2 fr", 
				"Description numéro 2", 
				"fr",
				Set.of(36), 
				Set.of(URI.create("http://www.example.condition.fr")),
				Set.of(new InternetAddress("info@example.updated.fr")),
				Set.of(URI.create("http://www.example.access1.fr"), URI.create("http://www.example.access2.fr")),
				Set.of(URI.create("http://www.example.download1.fr"), URI.create("http://www.example.download2.fr")),
				Collections.EMPTY_SET,
				Set.of(66),
				25,
				333,
				173,
				"Organisation 2 org fr updated",
				76,
				LocalDate.parse("2001-05-01", fmt),
				LocalDate.parse("2005-10-03", fmt));

		try {
			d10.updateDataset(d2, "fr");
		} catch (IOException ioe) {
			fail();
		}
	}

	@Test
	public void getDatasetNL() throws InterruptedException {
		try {
			Dataset did2 = d10.getDataset("id2", "nl");
			System.err.println(did2);
		} catch (IOException ioe) {
			fail();
		}
	}

	@Test
	public void getDatasetFR() throws InterruptedException {
		try {
			Dataset did2 = d10.getDataset("id2", "fr");
			System.err.println(did2);
		} catch (IOException ioe) {
			fail();
		}
	}

	@Test
	public void getDatasetsNL() throws InterruptedException {
		try {
			List<Dataset> lst = d10.getDatasets("nl");
			System.err.println(lst);
		} catch (IOException ioe) {
			fail();
		}
	}

	@Test
	public void getDatasetsFR() throws InterruptedException {
		try {
			List<Dataset> lst = d10.getDatasets("fr");
			System.err.println(lst);
		} catch (IOException ioe) {
			fail();
		}
	}

	public void deleteDataset() throws InterruptedException {
		try {
			d10.deleteDataset("id2");
		} catch (IOException ioe) {
			fail();
		}
	}
}
