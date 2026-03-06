/*
 * Copyright (c) 2022, FPS BOSA DG DT
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
package be.gov.data.scrapers;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * Custom SPARQL function to simplify (round to 1/50 degree, or about 1.5-2 km) a dcat:bbox polygon
 * 
 * @author Bart Hanssens
 */
public class SparqlSimplifyPolygon implements Function {
	public static final String NAMESPACE = "http://data.gov.be/sparql/function/";
	private static final Pattern P = 
		Pattern.compile("POLYGON\\(\\((-?\\d+.\\d+) (-?\\d+.\\d+),(-?\\d+.\\d+) (-?\\d+.\\d+),(-?\\d+.\\d+) (-?\\d+.\\d+),(-?\\d+.\\d+) (-?\\d+.\\d+),(-?\\d+.\\d+) (-?\\d+.\\d+)\\)\\)");
			

	@Override
	public String getURI() {
		return NAMESPACE + "simplifyPolygon";
	}

	private static double[] round(Matcher m) {
		double[] coord = new double[m.groupCount()];
		
		for (int i = 1; i <= m.groupCount(); i++) {
			coord[i-1] = Math.round(Double.parseDouble(m.group(i)) * 50) / 50f;
		}
		return coord;
	}

	@Override
	public Value evaluate(TripleSource tripleSource, Value... args) throws ValueExprEvaluationException {
		if (args.length != 1) {
			throw new ValueExprEvaluationException(
				"simplifyPolygon function requires exactly 1 argument, got " + args.length);
	    }

		Value arg = args[0];
	    // check if the argument is a literal, if not, we throw an error
	    if (!(arg instanceof Literal)) {
			throw new ValueExprEvaluationException("invalid argument (literal expected): " + arg);
	    }
		String str = ((Literal)arg).getLabel();
		Matcher matcher = P.matcher(str);
		if (matcher.matches()) {
			double[] coords = round(matcher);
			str = String.format(Locale.ENGLISH, 
				"POLYGON((%.2f %.2f, %.2f %.2f, %.2f %.2f, %.2f %.2f, %.2f %.2f))", 
				coords[0], coords[1], coords[2], coords[3], coords[4], coords[5], coords[6], coords[7],
				coords[8], coords[9]);
		}
		return Values.literal(str);
	}

	@Override
	public Value evaluate(ValueFactory vf, Value... values) throws ValueExprEvaluationException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}
}
