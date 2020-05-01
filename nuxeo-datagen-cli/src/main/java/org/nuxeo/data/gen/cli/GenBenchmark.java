package org.nuxeo.data.gen.cli;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.nuxeo.data.gen.meta.RandomDataGenerator;
import org.nuxeo.data.gen.meta.SequenceGenerator;
import org.nuxeo.ecm.core.blob.jit.gen.DocInfo;
import org.nuxeo.ecm.core.blob.jit.gen.InMemoryBlobGenerator;
import org.nuxeo.ecm.core.blob.jit.gen.StatementsBlobGenerator;
import org.nuxeo.importer.stream.message.DocumentMessage;


public class GenBenchmark {

	public static void main(String[] args) {

		Options options = new Options();
		options.addOption("t", "threads", true, "number of threads");
		options.addOption("i", "iterations", true, "number of iteration per threads");
		options.addOption("m", "messages", false, "generate full DocumentMessage");
		
		options.addOption("h", "help", false, "help");
		
		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
		}
		
		if (cmd.hasOption('h')) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("GenBenchmark", options);
			return;
		}

		int nbThreads = Integer.parseInt(cmd.getOptionValue('t', "16"));
		int nbIterations = Integer.parseInt(cmd.getOptionValue('i', "100000"));		

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nbThreads);
		executor.prestartAllCoreThreads();

		boolean generateMessage = cmd.hasOption('m');
		
		// force init
		SequenceGenerator.getDataGenerator();
		try {
			getGen();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		long t0 = System.currentTimeMillis();
		SequenceGenerator sg = new SequenceGenerator(60);
				
		
		for (int i = 0 ; i < nbThreads; i++) {
			executor.submit(new Runnable() {
				
				@Override
				public void run() {
					for (int n = 0; n < nbIterations; n++ ) {
						if (generateMessage) {
							try {
								next(sg);
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {
							sg.next().getMetaData();	
						}
						
					}					
				}
			});
		}
		
		
		executor.shutdown();
		boolean finished = false;
		while (!finished) {
			try {
				finished = executor.awaitTermination(500, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		long t1 = System.currentTimeMillis();
		
		long nbCalls = nbIterations*nbThreads;
		double throughput = 1000.0* (nbCalls)/(t1-t0);

		System.out.println("generated " + nbCalls + " entries with average throughput of " + throughput);
				
	}

	public static DocumentMessage next(SequenceGenerator sg) throws Exception {
		DocumentMessage ret;

		SequenceGenerator.Entry entry = sg.next();
		ret = createDocument("/", entry);
		
		return ret;
	}

	protected static StatementsBlobGenerator gen;
	
	protected static InMemoryBlobGenerator getGen() throws Exception{
		
		if (gen==null) {
			gen = new StatementsBlobGenerator();
			gen.initGenerator();
		}				
		return gen;
	}
	
	protected static DocumentMessage createDocument(String parentPath, SequenceGenerator.Entry entry) throws Exception {

		long currentAccountSeed = entry.getAccountKeyLong();
		long currentDataSeed = entry.getDataKey();
		int month = entry.getMonth();
		
		DocInfo docInfo = getGen().computeDocInfo("jit", currentAccountSeed, currentDataSeed, month);

		String title = getTitle(docInfo);
		String name = getName(title);

		HashMap<String, Serializable> props = new HashMap<>();
		props.put("dc:source", "initialImport");
		props.put("dc:title", title);		
		mapMetaData(props, docInfo);
		
		DocumentMessage.Builder builder = DocumentMessage.builder("Statement", parentPath, name).setProperties(props);
		
		// blobInfo.length can not be null and we do not yet know the actual size
		docInfo.blobInfo.length=-1L;
		builder.setBlobInfo(docInfo.blobInfo);

		DocumentMessage node = builder.build();
		return node;
	}

	protected static void mapMetaData(HashMap<String, Serializable> props, DocInfo docInfo) {
		props.put("statement:accountNumber", docInfo.getMeta("ACCOUNTID").trim());		
		try {
			Date stmDate = RandomDataGenerator.df.get().parse(docInfo.getMeta("DATE").trim());
			props.put("statement:statementDate", stmDate);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		String fullname = docInfo.getMeta("NAME").trim();
		int idx = fullname.indexOf(" ");		
		props.put("all:customerFirstname", fullname.substring(0, idx).trim());
		props.put("all:customerLastname", fullname.substring(idx).trim());
		
		Map<String, String> address = new HashMap<String, String>();
		address.put("city", docInfo.getMeta("CITY").trim());
		address.put("street", docInfo.getMeta("STREET").trim());		
		props.put("all:customerAddress", (Serializable) address);		

		props.put("all:customerNumber", docInfo.getMeta("ACCOUNTID").trim().substring(0,19));
	}

	protected static String getName(String title) {
		return title.replaceAll("\\W+", "-").toLowerCase();
	}

	protected static String getTitle(DocInfo docInfo) {
		return docInfo.getFileName();
	}

}
