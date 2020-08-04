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

public class RestCli {

	public static final String CONSUMERTREE = "consumertree";	
	public static final String IMPORT = "import";	
	
	private static final Map<String, String> opMap = new HashMap<String, String>();
    static {
        opMap.put("statementtree", "StreamImporter.runStatementFolderProducers");
        opMap.put("statements", "StreamImporter.runStatementProducers");
        opMap.put(CONSUMERTREE, "StreamImporter.runConsumerFolderProducers");
        opMap.put(IMPORT, "StreamImporter.runDocumentConsumers");        
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
		
		options.addOption("z", "batchSize", true, "Batch Size for Document Consumers");
		options.addOption("p", "logSize", true, "Number og partitions using in the stream");
		
		options.addOption( "bulk", false, "Enable bulkmode for import");
		
		
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
		int nbDocs = Integer.parseInt(cmd.getOptionValue('n', "100000"));
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
		
			if (cmd.hasOption("bulk")) {
				params.put("blockIndexing", true);	
			    params.put("blockDefaultSyncListeners", true);	
			}
			
			if (root!=null) {
				if (!root.startsWith("/")) {
					// avoid losing time debuging documents not being imported 
					root = "/" + root;
				}
				params.put("rootFolder", root);
			}		
		} else {
			params.put("nbDocuments", nbDocs);
			params.put("nbMonths", nbMonths);
			params.put("monthOffset", monthOffset);
			params.put("seed", seed);					
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
		op.voidOperation(true).execute();
		
		nuxeoClient.disconnect();
	}
	
	
	protected static void dumpParams(Map<String, Object> params) {
		
		for (String p: params.keySet()) {
			System.out.printf("   %s: %s \n", p, params.get(p).toString());
		}		
	}
	
	
}
