/*
 * Copyright (c) 2015, Bart Hanssens <bart.hanssens@fedict.be>
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
package be.fedict.dcat.helpers;

import be.fedict.dcat.vocab.DATAGOVBE;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVWriter;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.ParserConfig;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.XMLParserSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class Storage {
    private final Logger logger = LoggerFactory.getLogger(Storage.class);
    
    public final static String SKOLEM = "http://data.gov.be/.well-known/genid/";
    
    private Repository repo = null;
    private ValueFactory fac = null;
    private RepositoryConnection conn = null;
    
    /**
     * Get triple store.
     * 
     * @return 
     */
    public Repository getRepository() {
        return repo;
    }

    /**
     * Delete the triple store file.
     */
    public void deleteRepository() {
        if (repo != null) {
            logger.info("Removing RDF backend file");
    
            if(!repo.getDataDir().delete()) {
                logger.warn("Could not remove RDF backend file");
            }
            repo = null;
        }
    }
    
    /**
     * Get value factory.
     * 
     * @return 
     */
    public ValueFactory getValueFactory() {
        return fac;
    }

    /**
     * Create IRI using value factory.
     * 
     * @param str string
     * @return trimmed IRI 
     */
    public IRI getURI(String str) {
		try {
			return fac.createIRI(str.trim());
		} catch (IllegalArgumentException ioe) {
			logger.warn("Not a valid IRI {}, skolemizing", str);
			return skolemURI(fac.createBNode(str.trim()));
		}
    }
    
    /**
     * Replace blank node with skolem IRI
     * 
     * @param blank blank node
     * @return skolem IRI
     */
    public IRI skolemURI(BNode blank) {
        return fac.createIRI(SKOLEM + blank.getID());
    }
    
    /**
     * Check if a triple exists.
     * 
     * @param subj
     * @param pred
     * @return
     * @throws RepositoryException 
     */
    public boolean has(IRI subj, IRI pred) throws RepositoryException {
        return conn.hasStatement(subj, pred, null, false);
    }

	
    /**
     * Get multiple values from map structure.
     * 
     * @param map
     * @param prop
     * @param lang
     * @return 
     */
    public static List<String> getMany(Map<Resource, ListMultimap<String, String>> map, 
                                                        IRI prop, String lang) {
        List<String> res = new ArrayList<>();
        
        ListMultimap<String, String> multi = map.get(prop);
        if (multi != null && !multi.isEmpty()) {
            List<String> list = multi.get(lang);
            if (list != null && !list.isEmpty()) {
                res = list;
            }
        }
        return res;
    }
    
    /**
     * Get one value from map structure.
     * 
     * @param map
     * @param prop
     * @param lang
     * @return 
     */
    public static String getOne(Map<Resource, ListMultimap<String, String>> map, 
                                                    IRI prop, String lang) {
        String res = "";
        
        ListMultimap<String, String> multi = map.get(prop);
        if (multi != null && !multi.isEmpty()) {
            List<String> list = multi.get(lang);
            if (list != null && !list.isEmpty()) {
                res = list.get(0);
            }
        }
        return res;
    }
    
    
    /**
     * Add to the repository
     * 
     * @param in
     * @param format
     * @throws RepositoryException 
     * @throws RDFParseException 
     * @throws IOException 
     */
    public void add(InputStream in, RDFFormat format) 
            throws RepositoryException, RDFParseException, IOException {
        conn.add(in, DATAGOVBE.NAMESPACE, format, (Resource) null);
    }
    
    /**
     * Add an IRI property to the repository.
     * 
     * @param subj
     * @param pred
     * @param obj
     * @throws RepositoryException 
     */
    public void add(Resource subj, IRI pred, IRI obj) throws RepositoryException {
        conn.add(subj, pred, obj);
    }
    
    /**
     * Add an URL property to the repository.
     * 
     * @param subj
     * @param pred
     * @param url
     * @throws RepositoryException 
     */
    public void add(IRI subj, IRI pred, URL url) throws RepositoryException {
        String s = url.toString().replaceAll(" ", "%20");
        conn.add(subj, pred, fac.createIRI(s));
    }
    
    /**
     * Add a date property to the repository.
     * 
     * @param subj
     * @param pred
     * @param date
     * @throws RepositoryException 
     */
    public void add(IRI subj, IRI pred, Date date) throws RepositoryException {
        conn.add(subj, pred, fac.createLiteral(date));
    }
    
    /**
     * Add a string property to the repository.
     * 
     * @param subj
     * @param pred
     * @param value
     * @throws RepositoryException 
     */
    public void add(IRI subj, IRI pred, String value) throws RepositoryException {
		if ((value != null) && !value.isEmpty()) {
			conn.add(subj, pred, fac.createLiteral(value));
		} else {
			logger.warn("Skipping empty or null value for {} {}", subj, pred);
		}
    }
   
    /**
     * Add a language string property to the repository.
     * 
     * @param subj
     * @param pred
     * @param value
     * @param lang
     * @throws RepositoryException 
     */
    public void add(Resource subj, IRI pred, String value, String lang) 
                                                throws RepositoryException {
		if ((value != null) && !value.isEmpty()) {
			conn.add(subj, pred, fac.createLiteral(value, lang));
		} else {
			logger.warn("Skipping empty or null value for {} {}", subj, pred);
		}
    }

    /**
     * Add a typed property to the repository.
     * 
     * @param subj
     * @param pred
     * @param value
     * @param dtype
     * @throws RepositoryException 
     */
    public void add(Resource subj, IRI pred, String value, IRI dtype) 
                                                throws RepositoryException {
		if ((value != null) && !value.isEmpty()) {
			conn.add(subj, pred, fac.createLiteral(value, dtype));
		} else {
			logger.warn("Skipping empty or null value for {} {}", subj, pred);
		}
    }

    /**
     * Replace blank nodes with skolem URI
     */
    public void skolemize() {
        int i = 0;
        try (RepositoryResult<Statement> stmts = 
                                        conn.getStatements(null, null, null)) {
            while(stmts.hasNext()) {
                Statement stmt = stmts.next();
                Resource subject = stmt.getSubject();
                if (subject instanceof BNode) {
                    conn.add(skolemURI((BNode) subject), 
                                        stmt.getPredicate(), stmt.getObject());
                    conn.remove(stmt);
                    i++;
                }
                Value object = stmt.getObject();
                if (object instanceof BNode) {
                    conn.add(stmt.getSubject(), stmt.getPredicate(), 
                                                    skolemURI((BNode) object));
                    conn.remove(stmt);
                    i++;
                }
            }
        }
        logger.info("Replaced {} BNodes", i);
    }
    
    /**
     * Escape spaces in object URIs to avoid SPARQL issues
     * 
     * @param pred 
     * @throws RepositoryException 
     */
    public void escapeURI(IRI pred) throws RepositoryException {
        int i = 0;
        try (RepositoryResult<Statement> stmts = 
                                conn.getStatements(null, pred, null, false)) {
            while(stmts.hasNext()) {
                Statement stmt = stmts.next();
                Value val = stmt.getObject();
                // Check if object is Literal or URI
				if (val instanceof Resource) {
                    String uri = val.stringValue();
					if (uri.contains(" ") || uri.contains("\\") || uri.contains("[") || uri.contains("]")) {
                    // Check if URI contains a space or brackets
						if (uri.startsWith("[") && uri.endsWith("]")) {
							uri = uri.substring(1, uri.length() - 1);
						}
						String esc =  uri.replace(" ", "%20")
										.replace("\\\\", "%5c")
										.replace("\\", "%5c")
										.replace("[", "%5b")
										.replace("]", "%5d");
						logger.debug("Changing {} into {}", uri, esc);
						IRI obj = fac.createIRI(esc);
						conn.add(stmt.getSubject(), stmt.getPredicate(), obj);
						conn.remove(stmt);
						i++;
					}
                }
            }
        }
        logger.info("Replaced characters in {} URIs", i);
    }

    /**
     * Correct character encoding for literal. 
     * 
     * @param charset 
     */
    public void recode(Charset charset) {
        int i = 0;
        try (RepositoryResult<Statement> stmts = 
                                conn.getStatements(null, null, null, false)) {
            while(stmts.hasNext()) {
                Statement stmt = stmts.next();
                Value val = stmt.getObject();
                // Check if object is Literal or URI
                if (val instanceof Literal) {
                    String str = ((Literal) val).getLabel();
                    String newstr = new String(str.getBytes(charset), 
                                                StandardCharsets.UTF_8);
                    // Check if new string is different from old
                    if (!newstr.equals(str)) {
                        Optional<String> lang = ((Literal) val).getLanguage();
                        Literal newlit = lang.isPresent() 
                                        ? fac.createLiteral(newstr, lang.get())
                                        : fac.createLiteral(newstr);
                        conn.add(stmt.getSubject(), stmt.getPredicate(), newlit);
                        conn.remove(stmt);
                        i++;
                    }
                }
            }
        }
        logger.info("Replaced {} encoded strings", i);   
    }
    
    /**
     * Execute SPARQL Select query
     * 
     * @param sparql
     * @param out outputstream
     * @throws RepositoryException
     * @throws IOException
     */
    public void querySelect(String sparql, OutputStream out) 
                                    throws RepositoryException, IOException {
        try {
            TupleQuery select = conn.prepareTupleQuery(QueryLanguage.SPARQL, sparql);
            
            SPARQLResultsCSVWriter w = new SPARQLResultsCSVWriter(out);
            select.evaluate(w);
            
        } catch (MalformedQueryException | QueryEvaluationException ex) {
            throw new RepositoryException(ex);
        } catch (TupleQueryResultHandlerException ex) {
            throw new IOException(ex);
        }
    }
    /**
     * Execute SPARQL Update query
     * 
     * @param sparql
     * @throws RepositoryException
     */
    public void queryUpdate(String sparql) throws RepositoryException {
        try {
            Update upd = conn.prepareUpdate(QueryLanguage.SPARQL, sparql);
            upd.execute();
        } catch (MalformedQueryException | UpdateExecutionException  ex) {
            throw new RepositoryException(ex);
        }
    }
    
    /**
     * Get the list of all URIs of a certain class.
     * 
     * @param rdfClass
     * @return list of subjects
     * @throws RepositoryException 
     */
    public List<IRI> query(IRI rdfClass) throws RepositoryException {
        ArrayList<IRI> lst = new ArrayList<>();
        int i = 0;
 
        try (RepositoryResult<Statement> stmts = 
                        conn.getStatements(null, RDF.TYPE, rdfClass, false)) {
 
            if (! stmts.hasNext()) {
                logger.warn("No results for class {}", rdfClass.stringValue());
            }

            while(stmts.hasNext()) {
                Statement stmt = stmts.next();
                lst.add((IRI) stmt.getSubject());
                i++;
            }
        }
        logger.debug("Retrieved {} statements for {}", i, rdfClass.stringValue());
        return lst;
    }
    
    /**
     * Get a DCAT Dataset or a Distribution.
     * 
     * @param uri
     * @return 
     * @throws org.eclipse.rdf4j.repository.RepositoryException 
     */
    public Map<Resource, ListMultimap<String, String>> queryProperties(Resource uri) 
                                                throws RepositoryException  {
        Map<Resource, ListMultimap<String, String>> map = new HashMap<>();
        
        try (RepositoryResult<Statement> stmts = conn.getStatements(uri, null, null, true)) {
            if (! stmts.hasNext()) {
                logger.warn("No properties for {}", uri.stringValue());
            }
            
            while(stmts.hasNext()) {
                Statement stmt = stmts.next();
                IRI pred = stmt.getPredicate();
                Value val = stmt.getObject();
                
                String lang = "";
                if (val instanceof Literal) {
                    String l = ((Literal) val).getLanguage().orElse(null);
                    if (l != null) {
                        lang = l;
                    }
                }
                /* Handle multiple values for different languages */
                ListMultimap<String, String> multi = map.get(pred);
                if (multi == null) {
                    multi = ArrayListMultimap.create();
                    map.put(pred, multi);
                }
                multi.put(lang, val.stringValue());
            }
        }
        
        return map;
    }
    
    /**
     * Split property value into multiple values using a separator.
     * 
     * @param property
     * @param sep 
     * @throws org.eclipse.rdf4j.repository.RepositoryException 
     */
    public void splitValues(IRI property, String sep) throws RepositoryException {
        int i = 0;
        
        try (RepositoryResult<Statement> stmts = conn.getStatements(null, property, null, false)) {
            if (! stmts.hasNext()) {
                logger.warn("No property {}", property.stringValue());
            }
            while(stmts.hasNext()) {
                Statement stmt = stmts.next();
                Value value = stmt.getObject();
                
                // Only makes sense for literals
                if (value instanceof Literal) {
                    String l = ((Literal) value).getLanguage().orElse("");
                    String[] parts = ((Literal) value).stringValue().split(sep);
                    // Only do something when value can be splitted
                    if (parts.length > 1) {
                        for (String part : parts) {
                            Literal newval = fac.createLiteral(part.trim(), l);
                            conn.add(stmt.getSubject(), property, newval);
                        }
                        conn.remove(stmt);
						i++;
                    }
                }
            }
			conn.commit();
        }
        logger.debug("Splitted {} statements for '{}'", i, property.stringValue());
    }
    

    /**
     * Initialize RDF repository
     * 
     * @throws RepositoryException 
     */
    public void startup() throws RepositoryException {
        logger.info("Opening RDF repository");
        conn = repo.getConnection();
        fac = repo.getValueFactory();
		
		ParserConfig cfg = new ParserConfig();
		cfg.set(BasicParserSettings.VERIFY_URI_SYNTAX, false);
		cfg.set(XMLParserSettings.FAIL_ON_SAX_NON_FATAL_ERRORS, false);
/*		cfg.addNonFatalError(BasicParserSettings.VERIFY_RELATIVE_URIS);
		cfg.addNonFatalError(BasicParserSettings.VERIFY_URI_SYNTAX);
		cfg.addNonFatalError(XMLParserSettings.FAIL_ON_SAX_NON_FATAL_ERRORS); */
		conn.setParserConfig(cfg);
    }
    
    /**
     * Stop the RDF repository
     * 
     * @throws RepositoryException 
     */
    public void shutdown() throws RepositoryException {
        logger.info("Closing RDF repository");
        conn.commit();
        conn.close();
        repo.shutDown();
    }
    
    /**
     * Read contents of N-Triples input into a RDF repository
     * 
     * @param in
     * @throws RepositoryException 
     * @throws java.io.IOException 
     * @throws org.eclipse.rdf4j.rio.RDFParseException 
     */
    public void read(Reader in) throws RepositoryException, IOException, RDFParseException {
       this.read(in, RDFFormat.NTRIPLES);
    }
    
    /**
     * Read contents of input into a RDF repository
     * 
     * @param in
     * @param format RDF input format
     * @throws RepositoryException 
     * @throws IOException 
     * @throws RDFParseException 
     */
    public void read(Reader in, RDFFormat format) throws RepositoryException,
                                                IOException, RDFParseException {
        logger.info("Reading triples from input stream");
		ParserConfig cfg = new ParserConfig();
		cfg.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false);
		cfg.set(BasicParserSettings.VERIFY_URI_SYNTAX, false);
		conn.setParserConfig(cfg);
        conn.add(in, "http://data.gov.be", format);
    }
    
    /**
     * Write contents of RDF repository to N-Triples output
     * 
     * @param out
     * @throws RepositoryException 
     */
    public void write(Writer out) throws RepositoryException {
        this.write(out, RDFFormat.NTRIPLES);
    }    
     
    /**
     * Write contents of RDF repository to N-Triples output
     * 
     * @param out
     * @param format RDF output format
     * @throws RepositoryException 
     */
    public void write(Writer out, RDFFormat format) throws RepositoryException {
        RDFWriter writer = Rio.createWriter(RDFFormat.NTRIPLES, out);
        try {
            conn.export(writer);
        } catch (RDFHandlerException ex) {
            logger.warn("Error writing RDF");
        }
    }
       
    /**
     * RDF store
     * 
     * @param f file to be used as storage
     */
    public Storage(File f) {
        logger.info("Opening RDF store " + f.getAbsolutePath());
        
        MemoryStore mem = new MemoryStore(f);
        mem.setPersist(false);
        repo = new SailRepository(mem);
    }
}
