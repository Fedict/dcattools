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
public class MOBILITYDCAT {
	public final static IRI APPLICATION_LAYER_PROTOCOL = Values.iri("https://w3id.org/mobilitydcat-ap#applicationLayerProtocol");
	public final static IRI COMMUNICATION_METHOD = Values.iri("https://w3id.org/mobilitydcat-ap#communicationMethod");
	public final static IRI DATA_FORMAT_NOTES = Values.iri("https://w3id.org/mobilitydcat-ap#dataFormatNotes");
	public final static IRI GEOREFERENCING_METHOD = Values.iri("https://w3id.org/mobilitydcat-ap#georeferencingMethod");
	public final static IRI GRAMMAR = Values.iri("https://w3id.org/mobilitydcat-ap#grammar");
	public final static IRI HAS_MOBILITY_DATASET_STANDARD = Values.iri("https://w3id.org/mobilitydcat-ap#mobilityDataStandard");
	public final static IRI MOBILITY_THEME = Values.iri("https://w3id.org/mobilitydcat-ap#mobilityTheme");
	public final static IRI NETWORK_COVERAGE = Values.iri("https://w3id.org/mobilitydcat-ap#networkCoverage");
	public final static IRI TRANSPORT_MODE = Values.iri("https://w3id.org/mobilitydcat-ap#transportMode");
}
