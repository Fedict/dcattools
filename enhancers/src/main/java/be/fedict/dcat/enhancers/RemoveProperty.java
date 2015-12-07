/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.fedict.dcat.enhancers;

import static be.fedict.dcat.enhancers.AddProperty.LITERAL;
import be.fedict.dcat.helpers.Storage;
import java.util.List;
import java.util.regex.Matcher;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remove a property
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class RemoveProperty extends Enhancer {
    private final Logger logger = LoggerFactory.getLogger(RemoveProperty.class);
    
      /**
     * Remove a proerty
     * 
     * @param rdfClass
     * @param prop
     * @throws RepositoryException 
     */
    private void remove(URI rdfClass, URI prop) throws RepositoryException {
        logger.info("Remove {} from {}", prop.toString(), rdfClass.toString());
        
        Storage store = getStore();
        int removed = store.remove(rdfClass, prop);
        
        logger.info("Property removed {} times", Integer.toString(removed));
    } 
   
    
    
    @Override
    public void enhance() {
        try {
            URI rdfClass = getStore().getURI(getProperty("rdfclass"));
            URI property = getStore().getURI(getProperty("property"));
            
            remove(rdfClass, property);
        } catch (RepositoryException ex) {
            logger.error("Repository error", ex);
        }
    }
    
    /**
     * Adds a default property if the property is missing.
     * 
     * @param store 
     */
    public RemoveProperty(Storage store) {
        super(store);
    }
}
