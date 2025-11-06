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
public class DQV {
	public final static IRI HAS_QUALITY_ANNOTATION = Values.iri("http://www.w3.org/ns/dqv#hasQualityAnnotation");
	public final static IRI HAS_QUALITY_MEASUREMENT = Values.iri("http://www.w3.org/ns/dqv#hasQualityMeasurement");
	public final static IRI IS_MEASUREMENT_OF = Values.iri("http://www.w3.org/ns/dqv#isMeasurementOf");
	public final static IRI QUALITY_MEASUREMENT = Values.iri("http://www.w3.org/ns/dqv#QualityMeasurement");
	public final static IRI VALUE = Values.iri("http://www.w3.org/ns/dqv#value");
}
