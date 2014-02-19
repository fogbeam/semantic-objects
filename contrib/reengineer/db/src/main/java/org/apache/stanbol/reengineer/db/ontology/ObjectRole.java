package org.apache.stanbol.reengineer.db.ontology;

/* CVS $Id: ObjectRole.java 1082603 2011-03-17 17:41:13Z alexdma $ */
 
import com.hp.hpl.jena.rdf.model.*;
 
/**
 * Vocabulary definitions from http://ontologydesignpatterns.org/cp/owl/objectrole.owl 
 * @author Auto-generated by schemagen on 12 feb 2010 10:07 
 */
public class ObjectRole {
    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static Model m_model = ModelFactory.createDefaultModel();
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://www.ontologydesignpatterns.org/cp/owl/objectrole.owl#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    public static final Property hasRole = m_model.createProperty( "http://www.ontologydesignpatterns.org/cp/owl/objectrole.owl#hasRole" );
    
    public static final Property isRoleOf = m_model.createProperty( "http://www.ontologydesignpatterns.org/cp/owl/objectrole.owl#isRoleOf" );
    
    /** <p>Any physical, social, or mental object, or a substance</p> */
    public static final Resource Object = m_model.createResource( "http://www.ontologydesignpatterns.org/cp/owl/objectrole.owl#Object" );
    
    /** <p>A Concept that classifies an Object</p> */
    public static final Resource Role = m_model.createResource( "http://www.ontologydesignpatterns.org/cp/owl/objectrole.owl#Role" );
    
}
