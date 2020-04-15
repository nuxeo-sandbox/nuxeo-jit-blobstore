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
import org.nuxeo.client.NuxeoVersion;
import org.nuxeo.client.objects.Operation;
import org.nuxeo.client.objects.blob.Blob;
import org.nuxeo.client.objects.blob.FileBlob;
import org.nuxeo.data.gen.meta.SequenceGenerator;

public class RestCli {

	private static final Map<String, String> opMap = new HashMap<String, String>();
    static {
        opMap.put("statementtree", "StreamImporter.runStatementFolderProducers");
        opMap.put("statements", "StreamImporter.runStatementProducers");
        opMap.put("consumertree", "StreamImporter.runConsumerFolderProducers");
        opMap.put("consumers", "StreamImporter.runConsumerProducers");
        opMap.put("import", "StreamImporter.runDocumentConsumers");        
    }
    
	public static void main(String[] args) {

		Options options = new Options();
		options.addOption("o", "operation", true, "Operation: tree statements,customers,import");
		options.addOption("c", "config", true, "Configuration file - default is nuxeo.properties");
		options.addOption("t", "threads", true, "Number of threads");
		options.addOption("n", "nbDoc", true, "Number of Documents to generate");
		options.addOption("d", "months", true, "Number of months of statements to generate");
		options.addOption("h", "help", false, "Help");
		options.addOption("s", "seed", true, "Seed");
		options.addOption("f", "file", true, "location of CSV file to import");
		options.addOption("r", "root", true, "define the root where to import documents");

		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
		}

		String root = cmd.getOptionValue('r',null);
		
		String operation = cmd.getOptionValue('o',null);
		if (operation==null) {
			System.err.println("No Operation defined");
		} else {
			operation = operation.strip().toLowerCase();
		}

		if (cmd.hasOption('h') || operation==null) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("RestCLI", options);
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
		
		NuxeoClient nuxeoClient = createClient(nuxeoConfig);

		NuxeoVersion version =  nuxeoClient.getServerVersion();
		System.out.println("Connected to Nuxeo Server " + version.toString());
				
		int nbThreads = Integer.parseInt(cmd.getOptionValue('t', "10"));
		int nbDocs = Integer.parseInt(cmd.getOptionValue('n', "100000"));
		int nbMonths = Integer.parseInt(cmd.getOptionValue('d', "48"));
		long seed = Long.parseLong(cmd.getOptionValue('s', SequenceGenerator.DEFAULT_ACCOUNT_SEED + ""));

		if (!opMap.containsKey(operation)) {
			System.err.println("unknown operation " + operation);
			return;
		}
		
		String opId = opMap.get(operation);
		
		System.out.println("  Operation:" + opId);		
		System.out.println("  Threads:" + nbThreads);
		System.out.println("  nbDocs:" + nbDocs);
		System.out.println("  nbMonths:" + nbMonths);
		System.out.println("  seed:" + seed);
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("nbThreads", nbThreads);
		params.put("nbDocuments", nbDocs);
		params.put("nbMonths", nbMonths);
		params.put("nbThreads", nbThreads);
		params.put("seed", seed);
		if (root!=null) {
			if (!root.startsWith("/")) {
				// avoid losing time debuging documents not being imported 
				root = "/" + root;
			}
			params.put("rootFolder", root);
		}
		
		String csvFileName = cmd.getOptionValue('f',null);
		File csvFile=null;
		if (csvFileName!=null) {
			csvFile = new File(csvFileName);
		}
		
		Operation op = nuxeoClient.operation(opId).parameters(params);
		if (csvFile!=null) {
			Blob csvBlob = new FileBlob(csvFile);
			op.input(csvBlob);
		}
		op.voidOperation(true).execute();
		
		nuxeoClient.disconnect();
	}
	
	protected static NuxeoClient createClient(Properties config) {
		
		String url = config.getProperty("url");
		String login = config.getProperty("login");
		String pwd = config.getProperty("password");
		
		System.out.println("url=" + url);
		System.out.println("login=" + login);
		System.out.println("pwd=" + pwd);

		
		NuxeoClient nuxeoClient = new NuxeoClient.Builder()
                .url(url)
                .authentication(login, pwd)
                .readTimeout(24*3600)
                .connectTimeout(60)
                .transactionTimeout(24*3600)
                .connect();

		return nuxeoClient;
	}
}
