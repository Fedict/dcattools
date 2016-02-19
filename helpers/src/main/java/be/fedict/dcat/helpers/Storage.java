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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.SKOS;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.query.resultio.text.csv.SPARQLResultsCSVWriter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class Storage {
    private final Logger logger = LoggerFactory.getLogger(Storage.class);
    
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
     * Create URI using value factory.
     * 
     * @param str
     * @return 
     */
    public URI getURI(String str) {
        return fac.createURI(str.trim());
    }
    
    /**
     * Check if a triple exists.
     * 
     * @param subj
     * @param pred
     * @return
     * @throws RepositoryException 
     */
    public boolean has(URI subj, URI pred) throws RepositoryException {
        return conn.hasStatement(subj, pred, null, false);
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
     * Add an URI property to the repository.
     * 
     * @param subj
     * @param pred
     * @param obj
     * @throws RepositoryException 
     */
    public void add(URI subj, URI pred, URI obj) throws RepositoryException {
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
    public void add(URI subj, URI pred, URL url) throws RepositoryException {
        String s = url.toString().replaceAll(" ", "%20");
        conn.add(subj, pred, fac.createURI(s));
    }
    
    /**
     * Add a date property to the repository.
     * 
     * @param subj
     * @param pred
     * @param date
     * @throws RepositoryException 
     */
    public void add(URI subj, URI pred, Date date) throws RepositoryException {
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
    public void add(URI subj, URI pred, String value) throws RepositoryException {
        conn.add(subj, pred, fac.createLiteral(value));
    }
   
    /**
     * Add a string property to the repository.
     * 
     * @param subj
     * @param pred
     * @param value
     * @param lang
     * @throws RepositoryException 
     */
    public void add(URI subj, URI pred, String value, String lang) 
                                                throws RepositoryException {
        conn.add(subj, pred, fac.createLiteral(value, lang));
    }

    /**
     * Execute SPARQL Select query
     * 
     * @param sparql
     * @param out outputstream
     * @throws RepositoryException
     * @throws IOException
     */
    
    public void querySelect(String sparql, OutputStream out) throws RepositoryException, IOException
             {
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
    public List<URI> query(URI rdfClass) throws RepositoryException {
        ArrayList<URI> lst = new ArrayList<>();
        
        RepositoryResult<Statement> stmts = 
                conn.getStatements(null, RDF.TYPE, rdfClass, false);
     
        if (! stmts.hasNext()) {
            logger.warn("No results for class {}", rdfClass.stringValue());
        }
        
        int i = 0;
        while(stmts.hasNext()) {
            Statement stmt = stmts.next();
            lst.add((URI) stmt.getSubject());
            i++;
        }
        stmts.close();
        logger.debug("Retrieved {} statements for {}", i, rdfClass.stringValue());
        return lst;
    }
    
    /**
     * Get a DCAT Dataset or a Distribution.
     * 
     * @param uri
     * @return 
     * @throws org.openrdf.repository.RepositoryException 
     */
    public Map<URI, ListMultimap<String, String>> queryProperties(URI uri) 
                                                throws RepositoryException  {
        Map<URI, ListMultimap<String, String>> map = new HashMap<>();
        
        RepositoryResult<Statement> stmts = 
                conn.getStatements(uri, null, null, true);
        if (! stmts.hasNext()) {
            logger.warn("No properties for {}", uri.stringValue());
        }

        while(stmts.hasNext()) {
            Statement stmt = stmts.next();
            URI pred = stmt.getPredicate();
            Value val = stmt.getObject();
             
            String lang = "";
            if (val instanceof Literal) {
                String l = ((Literal) val).getLanguage();          
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
        stmts.close();
        
        return map;
    }
    
    /**
     * Split property value into multiple values using a separator.
     * 
     * @param property
     * @param sep 
     * @throws org.openrdf.repository.RepositoryException 
     */
    public void splitValues(URI property, String sep) throws RepositoryException {
        RepositoryResult<Statement> stmts = 
                conn.getStatements(null, property, null, false);
        
        if (! stmts.hasNext()) {
            logger.warn("No property {}", property.stringValue());
        }
        
        int i = 0;
        while(stmts.hasNext()) {
            Statement stmt = stmts.next();
            Value value = stmt.getObject();
            
            // Only makes sense for literals
            if (value instanceof Literal) {
                String l = ((Literal) value).getLanguage();
                String[] parts = ((Literal) value).stringValue().split(sep);
                // Only do something when value can be splitted
                if (parts.length > 1) {
                    for (String part : parts) {
                        Literal newval = fac.createLiteral(part.trim(), l);
                        conn.add(stmt.getSubject(), property, newval);
                    }
                    conn.remove(stmt);
                }
            }
            i++;
        }
        stmts.close();
        logger.debug("Retrieved {} statements for {}", i, property.stringValue());
    }
    

    /**
     * Initialize RDF repository
     * 
     * @throws RepositoryException 
     */
    public void startup() throws RepositoryException {
        logger.info("Opening RDF repository");
        repo.initialize(); 
        conn = repo.getConnection();
        fac = repo.getValueFactory();
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
     * Read contents of input into a RDF repository
     * 
     * @param in
     * @throws RepositoryException 
     * @throws java.io.IOException 
     * @throws org.openrdf.rio.RDFParseException 
     */
    public void read(Reader in) throws RepositoryException, IOException, RDFParseException {
       this.read(in, RDFFormat.NTRIPLES);
    }
    
    /**
     * Read contents of input into a RDF repository
     * 
     * @param in
     * @param format
     * @throws RepositoryException 
     * @throws java.io.IOException 
     * @throws org.openrdf.rio.RDFParseException 
     */
    public void read(Reader in, RDFFormat format) throws RepositoryException,
                                                IOException, RDFParseException {
        logger.info("Reading triples from input stream");
        conn.add(in, "http://data.gov.be", format);
    }
    
    /**
     * Write contents of RDF repository to output
     * 
     * @param out
     * @throws RepositoryException 
     */
    public void write(Writer out) throws RepositoryException {
        RDFWriter writer = Rio.createWriter(RDFFormat.NTRIPLES, out);
        try {
            conn.remove(null, SKOS.ALT_LABEL, null, new Resource[0]);
            conn.remove(null, SKOS.PREF_LABEL, null, new Resource[0]);
            conn.export(writer);
        } catch (RDFHandlerException ex) {
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
