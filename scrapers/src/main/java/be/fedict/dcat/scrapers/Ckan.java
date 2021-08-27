/*
 * Copyright (c) 2016, FPS BOSA DG DT
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

import java.io.File;
import java.net.URL;

/**
 * Abstract scraper for CKAN portals.
 *
 * @see http://ckan.org/
 *
 * @author Bart Hanssens
 */
public abstract class Ckan extends Scraper {

	// CKAN JSON fields
	public final static String RESULT = "result";
	public final static String SUCCESS = "success";
	public final static String PACKAGES = "packages";

	// CKAN API
	public final static String API_LIST = "/api/3/action/package_list";
	public final static String API_PKG = "/api/3/action/package_show?id=";
	public final static String API_ORG = "/api/3/action/organization_show?id=";
	public final static String API_RES = "/api/3/action/resource_show?id=";

	public final static String CATALOG = "/catalog";
	public final static String DATASET = "/dataset/";
	public final static String ORG = "/organization";
	public final static String RESOURCE = "/resource/";

	protected Ckan(File caching, URL base) {
		super(caching, base);
	}
}
