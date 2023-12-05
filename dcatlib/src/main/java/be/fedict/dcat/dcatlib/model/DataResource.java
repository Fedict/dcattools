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
package be.gov.data.dcatlib.model;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;

/**
 * Simplified DCAT Resource helper class
 * 
 * @author Bart Hanssens
 */
public class DataResource {
	private IRI iri;

	private String id;
	
	// multi-lingual values
	private Map<String,String> title;
	private Map<String,String> description;
	private Map<String,Set<String>> keywords;
	private Map<String,IRI> landingPage;
	private Map<String,String> contactName;
	private Map<String,IRI> contactAddr;	
	private Map<String,IRI> contactSite;
	
	// references
	private IRI creator;
	private IRI publisher;
	private IRI spatial;
	private IRI license;
	private Set<IRI> themes;
	private IRI accrualPeriodicity;

	private Date issued;
	private Date modified;
	private Date startDate;
	private Date endDate;
	
	/**
	 * @return the iri
	 */
	public IRI getIRI() {
		return iri;
	}

	/**
	 * @param iri the iri to set
	 */
	public void setIRI(IRI iri) {
		this.iri = iri;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the title
	 */
	public Map<String,String> getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(Map<String,String> title) {
		this.title = title;
	}

	/**
	 * @return the description
	 */
	public Map<String,String> getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(Map<String,String> description) {
		this.description = description;
	}

	/**
	 * @return the landingPage
	 */
	public Map<String,IRI> getLandingPage() {
		return landingPage;
	}

	/**
	 * @param landingPage the landingPage to set
	 */
	public void setLandingPage(Map<String,IRI> landingPage) {
		this.landingPage = landingPage;
	}

	/**
	 * @return the keywords
	 */
	public Map<String,Set<String>> getKeywords() {
		return keywords;
	}

	/**
	 * @param lang language code
	 * @return the keywords
	 */
	public Set<String> getKeywords(String lang) {
		return keywords.getOrDefault(lang, Collections.emptySet());
	}

	/**
	 * @param keywords the keywords to set
	 */
	public void setKeywords(Map<String,Set<String>> keywords) {
		this.keywords = keywords;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the contactName
	 */
	public Map<String,String> getContactName() {
		return contactName;
	}

	/**
	 * @param contactName the contactName to set
	 */
	public void setContactName(Map<String,String> contactName) {
		this.contactName = contactName;
	}

	/**
	 * @return the contactAddr
	 */
	public Map<String,IRI> getContactAddr() {
		return contactAddr;
	}

	/**
	 * @param contactAddr the contactAddr to set
	 */
	public void setContactAddr(Map<String,IRI> contactAddr) {
		this.contactAddr = contactAddr;
	}

	/**
	 * @return the contactSite
	 */
	public Map<String,IRI> getContactSite() {
		return contactSite;
	}

	/**
	 * @param contactSite the contactSite to set
	 */
	public void setContactSite(Map<String,IRI> contactSite) {
		this.contactSite = contactSite;
	}
	
	/**
	 * @return the issued
	 */
	public Date getIssued() {
		return issued;
	}

	/**
	 * @param issued the issued to set
	 */
	public void setIssued(Date issued) {
		this.issued = issued;
	}

	/**
	 * @return the modified
	 */
	public Date getModified() {
		return modified;
	}

	/**
	 * @param modified the modified to set
	 */
	public void setModified(Date modified) {
		this.modified = modified;
	}

	/**
	 * @return the creator
	 */
	public IRI getCreator() {
		return creator;
	}

	/**
	 * @param creator the creator to set
	 */
	public void setCreator(IRI creator) {
		this.creator = creator;
	}

	/**
	 * @return the publisher
	 */
	public IRI getPublisher() {
		return publisher;
	}

	/**
	 * @param publisher the publisher to set
	 */
	public void setPublisher(IRI publisher) {
		this.publisher = publisher;
	}

	/**
	 * @return the spatial
	 */
	public IRI getSpatial() {
		return spatial;
	}

	/**
	 * @param spatial the spatial to set
	 */
	public void setSpatial(IRI spatial) {
		this.spatial = spatial;
	}

	/**
	 * @return the license
	 */
	public IRI getLicense() {
		return license;
	}

	/**
	 * @param license the license to set
	 */
	public void setLicense(IRI license) {
		this.license = license;
	}

	/**
	 * @return the themes
	 */
	public Set<IRI> getThemes() {
		return themes;
	}

	/**
	 * @param themes the themes to set
	 */
	public void setThemes(Set<IRI> themes) {
		this.themes = themes;
	}

	/**
	 * @return the accrualPeriodicity
	 */
	public IRI getAccrualPeriodicity() {
		return accrualPeriodicity;
	}

	/**
	 * @param accrualPeriodicity the accrualPeriodicity to set
	 */
	public void setAccrualPeriodicity(IRI accrualPeriodicity) {
		this.accrualPeriodicity = accrualPeriodicity;
	}

	/**
	 * @return the startDate
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return the endDate
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

}
