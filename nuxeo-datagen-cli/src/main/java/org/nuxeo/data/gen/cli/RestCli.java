package org.nuxeo.data.gen.cli;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.nuxeo.client.NuxeoClient;
import org.nuxeo.client.NuxeoClient.Builder;
import org.nuxeo.client.NuxeoVersion;
import org.nuxeo.client.objects.Operation;
import org.nuxeo.client.objects.blob.Blob;
import org.nuxeo.client.objects.blob.FileBlob;
import org.nuxeo.data.gen.meta.SequenceGenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestCli {

	public static final String CONSUMERTREE = "consumertree";	
	public static final String IMPORT = "import";	
	//public static final String IMPORTEX = "importEx";	
	public static final String STATEMENT = "statements";	
	
	private static final Map<String, String> opMap = new HashMap<String, String>();
    static {
        opMap.put("statementtree", "StreamImporter.runStatementFolderProducers");
        opMap.put(STATEMENT, "StreamImporter.runStatementProducers");
        opMap.put(CONSUMERTREE, "StreamImporter.runConsumerFolderProducers");
        opMap.put(IMPORT, "StreamImporter.runDocumentConsumersEx");   
        //opMap.put(IMPORTEX, "StreamImporter.runDocumentConsumersEx");   
    }

    protected static void help(Options options) {
    	HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("RestCLI", options);
    }
    
	public static void main(String[] args) {

		Options options = new Options();
		options.addOption("o", "operation", true, "Operation: tree statements,customers,import");
		options.addOption("c", "config", true, "Configuration file - default is nuxeo.properties");
		options.addOption("t", "threads", true, "Number of threads");
		options.addOption("n", "nbDoc", true, "Number of Documents to generate");
		options.addOption("d", "months", true, "Number of months of statements to generate");
		options.addOption("monthOffset", true, "Months offset");
		options.addOption("h", "help", false, "Help");
		options.addOption("s", "seed", true, "Seed");
		
		options.addOption("r", "repo", true, "define the target repository");
		options.addOption("b", "root", true, "define the root/basepath where to import documents");
		options.addOption("l", "logName", true, "name (or prefix) of the stream to use");
		options.addOption("m", "multiRepo", false, "Define if multi-repositories is used");
		options.addOption("a", "async", false, "Call Automation using the @async adapter");
		options.addOption("w", "wait", true, "Pools server until async execution is completed");
		
		options.addOption("z", "batchSize", true, "Batch Size for Document Consumers");
		options.addOption("p", "logSize", true, "Number og partitions using in the stream");
	
		options.addOption("skip", true, "Number of entries to skip in the random sequence");
		
		options.addOption( "bulk", false, "Enable bulkmode for import");
		options.addOption( "storeInRoot", false, "Generate DocumentMessage that will have / as parent");
		
		options.addOption( "useScroller", false, "Trigger Scroller for Bulk Indexing");
		
		
		options.addOption( "smartPartitioning", false, "Statement messages in partitions in a way that optimize cache usage in DBS");		
		options.addOption( "json", true, "JSON string to pass additional parameters to the server");		
		
		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
		}
		
		if (cmd.hasOption('h')) {
			help(options);
			return;
		}
		
		String operation = cmd.getOptionValue('o',null);
		if (operation==null) {
			System.err.println("No Operation defined");
			help(options);
			return;
		} else {
			operation = operation.trim().toLowerCase();
		}
	
		if (!opMap.containsKey(operation)) {
			System.err.println("unknown operation " + operation);		
			return;
		}

	
		Properties nuxeoConfig = new Properties();
		String config = cmd.getOptionValue('c', "nuxeo.properties");
		try {			
			File configFile = new File(config);			
			System.out.println("Using config " + configFile.getAbsolutePath());
			nuxeoConfig.load(new FileReader(configFile));
		} catch (Exception e) {
			System.err.println("Unable to read configuration file " + e.getMessage());
			return;
		}
		

		int logSize = Integer.parseInt(cmd.getOptionValue('p', "8"));		
		int batchSize = Integer.parseInt(cmd.getOptionValue('z', "500"));		
		int nbThreads = Integer.parseInt(cmd.getOptionValue('t', "10"));
		long nbDocs = Long.parseLong(cmd.getOptionValue('n', "100000"));
		int nbMonths = Integer.parseInt(cmd.getOptionValue('d', "48"));
		int monthOffset = Integer.parseInt(cmd.getOptionValue("monthOffset", "0"));
		String root = cmd.getOptionValue('b',null);
		String repo = cmd.getOptionValue('r',null);
		long seed = Long.parseLong(cmd.getOptionValue('s', SequenceGenerator.DEFAULT_ACCOUNT_SEED + ""));

		String logName = cmd.getOptionValue('l', null);
		boolean split=false;
		if (cmd.hasOption("m")) {
			split=true;
		}
		
		String opId = opMap.get(operation);
		boolean async = false;
		if (cmd.hasOption("a")) {
			async=true;
		}		
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("logName", logName);
		params.put("logSize", logSize);
		params.put("nbThreads", nbThreads);		

		
		// allow to pass additional arbitrary parameters
		String jsonParams = cmd.getOptionValue("json",null);
		if (jsonParams!=null && !jsonParams.isEmpty()) {			
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node;
			try {
				node = mapper.readTree(jsonParams);
				Map<String, Object> additionalParams = mapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
				params.putAll(additionalParams);
			} catch (Exception e) {
				System.err.println("Unable to parse json parameter" + e.getMessage());
				System.err.println(jsonParams);				
			} 				
		}
		
		if (CONSUMERTREE.equalsIgnoreCase(operation)) {
			// force single thread
			nbThreads = 1;
			if (logName==null) {
				logName = "import/hierarchy";
			}
			params.put("logName", logName);
			params.put("split", split);
		} else if (IMPORT.equalsIgnoreCase(operation)) {

			if (logName==null) {
				System.err.println("You should provide a logName (-l)");
				return;
			}
			params.put("batchSize", batchSize);
	
		    params.put("blockDefaultSyncListeners", true);				
			if (cmd.hasOption("bulk")) {
				params.put("blockIndexing", true);	
				params.put("blockAsyncListeners", true);        
				params.put("blockPostCommitListeners", true);
			}
			
			if (cmd.hasOption("useScroller")) {
				params.put("useScroller", true);
			}				
			
			if (root!=null) {
				if (!root.startsWith("/")) {
					// avoid losing time debuging documents not being imported 
					root = "/" + root;
				}
				params.put("rootFolder", root);
			}		
		} else {
			// Statement producer
			if (logName==null) {
				logName = "import/statements";
			}
			params.put("logName", logName);
			params.put("split", split);
			params.put("nbDocuments", nbDocs);
			params.put("nbMonths", nbMonths);
			params.put("monthOffset", monthOffset);
			
			if (cmd.hasOption("storeInRoot")) {
				params.put("storeInRoot", true);
			} else {
				params.put("storeInCustomerFolder", true);	
			}
			if (cmd.hasOption("smartPartitioning")) {
				params.put("smartPartitioning", true);
			} 
			
						
			params.put("seed", seed);		
			
			int skip = Integer.parseInt(cmd.getOptionValue("skip", "0"));
			params.put("skip", skip);		
			
		}
		

		NuxeoClient nuxeoClient = NuxeoClientHelper.createClient(nuxeoConfig, async);
		NuxeoVersion version =  nuxeoClient.getServerVersion();
		System.out.println("Connected to Nuxeo Server " + version.toString());
		
		
		System.out.println("Running Operation:" + opId);		
		dumpParams(params);		
					
		Operation op = nuxeoClient.operation(opId).parameters(params);
		if (repo!=null) {
		  op = op.header("X-NXRepository", repo); 
		}
		String result = (String) op.execute();
		System.out.println("#####################");
		if (result!=null) {
			System.out.println("Execution completed");
			System.out.println(ResponseHelper.formatAsString(result));
		}
		
		if (async && cmd.hasOption("w")) {
			System.out.println("waiting for end of Async Exec");
			int timeout = Integer.parseInt(cmd.getOptionValue('w', ""+12*3600));				
			NuxeoClientHelper.waitForResult(nuxeoConfig, 60, timeout);	
		}
		
		nuxeoClient.disconnect();
	}
	
	
	protected static void dumpParams(Map<String, Object> params) {
		
		for (String p: params.keySet()) {
			System.out.printf("   %s: %s \n", p, params.get(p).toString());
		}		
	}
	
	
}
