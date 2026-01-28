/*
 * Copyright (c) 2025, FPS BOSA
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
package be.gov.data.dcat.vocab;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

/**
 *
 * @author Bart.Hanssens
 */
public class GEODCAT {
	public final static IRI CUSTODIAN = Values.iri("http://data.europa.eu/930/custodian");
	public final static IRI DISTRIBUTOR = Values.iri("http://data.europa.eu/930/distributor");
	public final static IRI ORIGINATOR = Values.iri("http://data.europa.eu/930/originator");
	public final static IRI PROCESSOR = Values.iri("http://data.europa.eu/930/processor");
	public final static IRI REFERENCE_SYSTEM = Values.iri("http://data.europa.eu/930/referenceSystem");
	public final static IRI RESOURCE_PROVIDER = Values.iri("http://data.europa.eu/930/resourceProvider");
	public final static IRI RESOURCE_TYPE = Values.iri("http://data.europa.eu/930/resourceType");
	public final static IRI SERVICE_TYPE = Values.iri("http://data.europa.eu/930/serviceType");
	public final static IRI SERVICE_PROTOCOL = Values.iri("http://data.europa.eu/930/serviceProtocol");
	public final static IRI SPATIAL_RESOLUTION_AS_SCALE = Values.iri("http://data.europa.eu/930/spatialResolutionAsScale");
	public final static IRI TOPIC_CATEGORY = Values.iri("http://data.europa.eu/930/topicCategory");
}
