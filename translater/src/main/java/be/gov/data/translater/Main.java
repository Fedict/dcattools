/*
 * Copyright (c) 2023, FPS BOSA DG DT
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
package be.gov.data.translater;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Translates literals by calling external etranslation proxy (which uses EU eTranslation)
 * 
 * @author Bart Hanssens
 */
@Command(name="main", mixinStandardHelpOptions=true, description="Translate")
public class Main implements Callable<Integer> {
	private final static Logger LOG = LoggerFactory.getLogger(Main.class);

	@Option(names = {"-f", "--file"}, description = "Input file", required = true)
	private Path inFile;

	@Option(names = {"-t", "--translated"}, description = "Translated output file", required = true)
	private Path outFile;

	@Option(names={"-l", "--language"}, description="target languages")
	private List<String> targets;

	@Option(names={"-U", "--url"}, description="etranslation URI", required=true)
	private String url;

	@Option(names={"-u", "--user"}, description="etranslation user")
	private String user;

	@Option(names={"-p", "--pass"}, description="etranslation pass")
	private String pass;

	@Option(names={"-c", "--cache"}, description="Use existing output file as cache")
	private boolean cache;

	@Override
	public Integer call() throws Exception { 
		LOG.info("-- START --");

		Translater t = new Translater(url, user, pass);
		
		if (cache || outFile.toFile().exists()) {
			LOG.info("Use previous file as cache");
			try(InputStream is = Files.newInputStream(outFile)) {
				t.cache(is);
			} catch (IOException ioe) {
				LOG.warn(ioe.getMessage());
			}
		}
		
		try(InputStream is = Files.newInputStream(inFile);
			OutputStream os = Files.newOutputStream(outFile)){
			t.translate(is, os, targets);
		} catch (IOException ioe) {
			LOG.error(ioe.getMessage());
		}
		LOG.info("-- END --");
		return 0;
	}

	/**
	 * Main
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
	}
}
