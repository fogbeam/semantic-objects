package org.apache.stanbol.reasoners.web.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.owl.transformation.JenaToClerezzaConverter;
import org.apache.stanbol.owl.transformation.OWLAPIToClerezzaConverter;
import org.apache.stanbol.reasoners.jena.JenaReasoningService;
import org.apache.stanbol.reasoners.owlapi.OWLApiReasoningService;
import org.apache.stanbol.reasoners.servicesapi.InconsistentInputException;
import org.apache.stanbol.reasoners.servicesapi.ReasoningService;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceException;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceInputManager;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceInputProvider;
import org.apache.stanbol.reasoners.servicesapi.UnsupportedTaskException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.SWRLRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.rulesys.Rule;

/**
 * TODO Add comment
 */
public class ReasoningServiceExecutor implements Callable<ReasoningServiceResult<?>> {
	private Logger log = LoggerFactory.getLogger(getClass());
	private TcManager tcManager;
	private ReasoningServiceInputManager inmgr;
	private ReasoningService<?,?,?> service;
	private String task;
	private Map<String,List<String>> parameters;
	private String targetGraph;
	
	// This task is not dinamically provided by the service, since it work on a
	// specific method
	// (isConsistent())
	public static String TASK_CHECK = "check";

    public ReasoningServiceExecutor(TcManager tcManager,
                                    ReasoningServiceInputManager inmgr,
                                    ReasoningService<?,?,?> service,
                                    String task,
                                    String targetGraph,
                                    Map<String,List<String>> parameters) {
		this.tcManager = tcManager;
		this.inmgr = inmgr;
		this.service = service;
		this.task = task;
		this.targetGraph = targetGraph;
		this.parameters = parameters;
	}

	/**
	 * Execute a JenaReasoningService
	 * 
	 * TODO: Add parameter to decide if the output graph must be deleted if
	 * exists
	 * 
	 * @param s
	 * @param input
	 * @param rules
	 * @return
	 * @throws ReasoningServiceException 
	 * @throws UnsupportedTaskException 
	 */
	private ReasoningServiceResult<Model> executeJenaReasoningService(String task,
			JenaReasoningService s, Model input, List<Rule> rules,
			String targetGraphID, boolean filtered,
			Map<String, List<String>> parameters) throws ReasoningServiceException, UnsupportedTaskException {
	    
        // Check task: this is managed directly by the endpoint
		if (task.equals(ReasoningServiceExecutor.TASK_CHECK)) {
			log.debug("Task is '{}'", ReasoningServiceExecutor.TASK_CHECK);
			try {
			    boolean is = s.isConsistent(input);
				return new ReasoningServiceResult<Model>(ReasoningServiceExecutor.TASK_CHECK,is);
			} catch (ReasoningServiceException e) {
				log.error("Error thrown: {}", e);
				throw e;
			}
		}
		try {
			Set<Statement> result = s.runTask(task, input, rules, filtered,
					parameters);
			if (result == null) {
				log.error("Result is null");
				throw new RuntimeException("Result is null.");
			}
			Model outputModel = ModelFactory.createDefaultModel();
			outputModel.add(result.toArray(new Statement[result.size()]));
			// If target is null, then get back results, elsewhere put it in
			// target graph

            log.info("Prepare output");
			if (targetGraphID == null) {
				log.info("Returning {} statements", result.size());
				return new ReasoningServiceResult<Model>(task, true, outputModel);
			} else {
				save(outputModel, targetGraphID);
				return new ReasoningServiceResult<Model>(task, true);
			}
		} catch (ReasoningServiceException e) {
			log.error("Error thrown: {}", e);
			throw e;
		} catch (InconsistentInputException e) {
			log.debug("The input is not consistent");
			return new ReasoningServiceResult<Model>(ReasoningServiceExecutor.TASK_CHECK, false);
		} catch (UnsupportedTaskException e) {
			log.error("Error thrown: {}", e);
			throw e;
		} catch (IOException e) {
            throw new ReasoningServiceException(e);
        }
	}

	/**
	 * Executes the OWLApiReasoingService
	 * 
	 * @param task
	 * @param s
	 * @param input
	 * @param rules
	 * @param targetGraphID
	 * @param parameters
	 * @return
	 * @throws InconsistentInputException 
	 * @throws ReasoningServiceException 
	 * @throws UnsupportedTaskException 
	 */
	private ReasoningServiceResult<OWLOntology> executeOWLApiReasoningService(String task,
			OWLApiReasoningService s, OWLOntology input, List<SWRLRule> rules,
			String targetGraphID, boolean filtered, Map<String, List<String>> parameters) throws InconsistentInputException, ReasoningServiceException, UnsupportedTaskException {

		// Check task: this is managed directly by the endpoint
		if (task.equals(ReasoningServiceExecutor.TASK_CHECK)) {
			log.debug("Task is '{}'", ReasoningServiceExecutor.TASK_CHECK);
			try {
			    boolean is = s.isConsistent(input);
				return new ReasoningServiceResult<OWLOntology>(ReasoningServiceExecutor.TASK_CHECK, is);
			} catch (ReasoningServiceException e) {
				throw e;
			}
		}
		// We get the manager from the input ontology
		// XXX We must be aware of this.
		OWLOntologyManager manager = input.getOWLOntologyManager();
		try {
			OWLOntology output = manager.createOntology();
			Set<OWLAxiom> axioms = s.runTask(task, input, rules, filtered,
                parameters);
            log.info("Prepare output: {} axioms",axioms.size());
			manager.addAxioms(output,axioms);
            if (targetGraphID == null) {
                return new ReasoningServiceResult<OWLOntology>(task, true, manager.getOntology(output.getOntologyID()));
			} else {
				save(output, targetGraphID);
				return new ReasoningServiceResult<OWLOntology>(task, true);
			}
		} catch (InconsistentInputException e) {
            log.warn("The input is not consistent");
            throw e;
        } catch (ReasoningServiceException e) {
			throw e;
		} catch (OWLOntologyCreationException e) {
		    log.error("Error! \n",e);
			throw new ReasoningServiceException(new IOException(e));
		} catch (UnsupportedTaskException e) {
		    log.error("Error! \n",e);
			throw e;
		}catch(Throwable t){
		    log.error("Error! \n",t);
		    throw new ReasoningServiceException(t);
		}
	}


	/**
	 * To save data in the triple store.
	 * 
	 * @param data
	 * @param targetGraphID
	 * @throws IOException 
	 */
	protected void save(Object data, String targetGraphID) throws IOException {
		log.info("Attempt saving in target graph {}", targetGraphID);
		
		final long startSave = System.currentTimeMillis();
		LockableMGraph mGraph;
		UriRef graphUriRef = new UriRef(targetGraphID);

		// tcManager must be synchronized
		synchronized (tcManager) {
    		try {
                // Check whether the graph already exists
                mGraph = tcManager.getMGraph(graphUriRef);
            } catch (NoSuchEntityException e) {
                mGraph = tcManager.createMGraph(graphUriRef);
            }
        }
		
		// We lock the graph before proceed
		Lock writeLock = mGraph.getLock().writeLock();
		boolean saved = false;
		if (data instanceof Model) {
			MGraph m = JenaToClerezzaConverter
					.jenaModelToClerezzaMGraph((Model) data);
			writeLock.lock();
			saved = mGraph.addAll(m);
			writeLock.unlock();
		} else if (data instanceof OWLOntology) {
			MGraph m = (MGraph) OWLAPIToClerezzaConverter
					.owlOntologyToClerezzaMGraph((OWLOntology) data);
			writeLock.lock();
			saved = mGraph.addAll(m);
			writeLock.unlock();
		}
		if (!saved)
			throw new IOException(
					"Cannot save the result in clerezza!");
		final long endSave = System.currentTimeMillis();
		log.info("Saved in time: {}ms", (endSave - startSave));
	}

	
    private ReasoningServiceResult<?> execute(String task,
        ReasoningService<?,?,?> service,
        String targetGraphID,
        Map<String,List<String>> parameters) throws ReasoningServiceException,
                                            UnsupportedTaskException, InconsistentInputException {
        

        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        long start = System.currentTimeMillis();
        log.info("[start] Execution: {}",service.getClass().getCanonicalName());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        log.info("-----------------------------------------------------");
        log.info("execute()");
        log.info(" > task: {}", task);
        log.info(" > service: {}", service.getClass().getCanonicalName());
        log.info(" > target: {}", targetGraphID);
        log.info(" > parameters:");
        for(Entry<String,List<String>> e : parameters.entrySet()){
            log.info(" >> {}: {}",e.getKey());
            for(String v: e.getValue()){
                log.info(" >>> value: {}",v);
            }
        }
        log.info(" > input providers:");
        for(ReasoningServiceInputProvider p : inmgr.getProviders()){
            log.info(" >> {}", p.getClass().getCanonicalName());
        }
        log.info("-----------------------------------------------------");
        
        ReasoningServiceResult<?> result = null;
        /**
         * TODO Switch this into the ReasoningService implementation
         */
        if (service instanceof JenaReasoningService) {
            Model input = ModelFactory.createDefaultModel();
            synchronized (inmgr) {
                Iterator<Statement> statements = inmgr.getInputData(Statement.class);
                while (statements.hasNext()) {
                    input.add(statements.next());
                }
            }
            List<Rule> rules = null;
            synchronized (inmgr) {
                Iterator<Rule> rulesI = inmgr.getInputData(Rule.class);
                while (rulesI.hasNext()) {
                    Rule o = rulesI.next();
                    log.info(">>>>>RULE>>>>>>>>>> {}",o);
                    if(rules == null){
                        rules = new ArrayList<Rule>();
                    }
                    rules.add(o);
                }
            }
            log.info("Input size is {} statements",input.listStatements().toSet().size());
            result = executeJenaReasoningService(task, (JenaReasoningService) service, input, rules,
                targetGraphID, true, parameters);
        } else if (service instanceof OWLApiReasoningService) {
            OWLOntology input;
            try {
                input = OWLManager.createOWLOntologyManager().createOntology();
            } catch (OWLOntologyCreationException e) {
                throw new ReasoningServiceException(e);
            }
            synchronized (inmgr) {
                Iterator<OWLAxiom> statements = inmgr.getInputData(OWLAxiom.class);
                while (statements.hasNext()) {
                    input.getOWLOntologyManager().addAxiom(input, statements.next());
                }
            }
            // FIXME Please check if this is really necessary!!!
            input = input.getOWLOntologyManager().getOntology(input.getOntologyID());
            List<SWRLRule> rules = null;
            synchronized (inmgr) {
                Iterator<SWRLRule> rulesI = inmgr.getInputData(SWRLRule.class);
                while (rulesI.hasNext()) {
                    if(rules == null){
                        rules = new ArrayList<SWRLRule>();
                    }
                    rules.add(rulesI.next());
                }
            }
            log.info("Input size is {} statements",input.getAxiomCount());
            result = executeOWLApiReasoningService(task, (OWLApiReasoningService) service, input, rules,
                targetGraphID, true, parameters);

        } else throw new UnsupportedOperationException("Service implementation not supported!");

        log.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        long end = System.currentTimeMillis();
        log.info("[end] In time: {}ms", (end - start));
        log.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        
        return result;
    }

    @Override
    public ReasoningServiceResult<?> call() throws Exception {
        return execute(task,service,targetGraph,parameters);
    }

}
