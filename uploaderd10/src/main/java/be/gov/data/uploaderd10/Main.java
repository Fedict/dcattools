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

package be.gov.data.uploaderd10;

import be.gov.data.uploaderd10.drupal.Comparer;
import be.gov.data.uploaderd10.drupal.DrupalClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.PropertiesDefaultProvider;

/**
 *
 * @author Bart Hanssens
 */
@Command(name = "uploader", mixinStandardHelpOptions = true, description = "Uploads DCAT data to Drupal 10.")
public class Main implements Callable<Integer> {
	@Option(names = {"-u", "--user"}, description = "User name")
    private String user;

	@Option(names = {"-p", "--password"}, description = "Password")
    private String pass;
 
	@Option(names = {"-U", "--url"}, description = "Drupal site")
    private String site;
 
	@Option(names = {"-f", "--file"}, description = "RDF file")
    private Path file;

	@Option(names = {"-P", "--properties"}, description = "Property file")
	private void setProperties(File f) {
		if (f == null || !f.canRead()) {
			return;
		}
        System.setProperty("picocli.defaults.uploader.path", f.toString());
	}
 
	@Override
	public Integer call() throws Exception {
		DrupalClient cl = new DrupalClient(site);
		try {
			cl.login(user, pass);
			Comparer comparer = new Comparer(cl);
			comparer.compare(new String[]{"nl", "fr", "de", "en"});
			return 0;
		} finally {
			cl.logout();
		}
	}

	/**
	 * Main
	 * 
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		CommandLine cl = new CommandLine(new Main());
		cl.setDefaultValueProvider(new PropertiesDefaultProvider());
		int exitCode = cl.execute(args);
	    System.exit(exitCode);
	}

}
