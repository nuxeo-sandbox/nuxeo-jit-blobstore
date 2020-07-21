package org.nuxeo.data.gen.cli;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
import org.nuxeo.client.objects.blob.StreamBlob;

public class CSVRestImporter {

	protected static final int PAGE_SIZE = 1000;

	public static void main(String[] args) {

		Options options = new Options();
		options.addOption("c", "config", true, "Configuration file - default is nuxeo.properties");
		options.addOption("t", "threads", true, "Number of threads");
		options.addOption("h", "help", false, "Help");
		options.addOption("f", "file", true, "location of CSV file to import");

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

		if (nuxeoClient!=null) {
			NuxeoVersion version = nuxeoClient.getServerVersion();
			System.out.println("Connected to Nuxeo Server " + version.toString());
		}

		int nbThreads = Integer.parseInt(cmd.getOptionValue('t', "10"));

		String csvFileName = cmd.getOptionValue('f', null);
		File csvFile = null;
		if (csvFileName != null) {
			csvFile = new File(csvFileName);
		} else {
			System.err.println("No imput file");
			return;
		}

		long t0 = System.currentTimeMillis();

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nbThreads);
		executor.prestartAllCoreThreads();

		System.out.println("Started ThreadPool with " + nbThreads + " threads");

		FileInputStream inputStream = null;
		Scanner sc = null;

		StringBuffer page = new StringBuffer();
		long nbLines = 0;
		int batches = 0;
		try {
			inputStream = new FileInputStream(csvFile);
			sc = new Scanner(inputStream, "UTF-8");
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				nbLines++;
				page.append(line).append("\n");
				if (nbLines % PAGE_SIZE == 0) {
					executor.submit(mkTask(page.toString(), nuxeoClient));
					batches++;
					System.out.println("Send batch " + batches);
					page = new StringBuffer();
				}
			}
			if (sc.ioException() != null) {
				throw sc.ioException();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (sc != null) {
				sc.close();
			}
		}

		executor.shutdown();
		boolean finished = false;
		Long throughput;
		long pauseTimeS = 2;

		while (!finished) {

			long t1 = System.currentTimeMillis();
			int threads = executor.getActiveCount();

			long elapsed = (t1 - t0);
			throughput = Math.round(nbLines * 1.0 / (elapsed / 1000));

			System.out.println("   Throughput:" + throughput + " d/s using " + threads + " threads");
			System.out.println(" Queue size: " + executor.getQueue().size());
			System.out.println(" Tasks count: " + executor.getTaskCount());
			
			try {
				finished = executor.awaitTermination(pauseTimeS, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (nuxeoClient!=null) {
			nuxeoClient.disconnect();
		}
	}

	protected static Runnable mkTask(String csv, NuxeoClient client) {
		return new Runnable() {

			@Override
			public void run() {
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("nbThreads", 1);
				params.put("logName", "import/Customers");
				params.put("split", true);

				if (client == null) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					Operation op = client.operation("StreamImporter.runConsumerProducers").parameters(params);
					Blob csvBlob = new StreamBlob(new ByteArrayInputStream(csv.getBytes()), "input.csv");
					op.input(csvBlob);
					op.voidOperation(true).execute();
				}
			}

		};
	}

	protected static NuxeoClient createClient(Properties config) {

		String url = config.getProperty("url");
		String login = config.getProperty("login");
		String pwd = config.getProperty("password");

		System.out.println("url=" + url);
		System.out.println("login=" + login);
		System.out.println("pwd=" + pwd);

		if (url == null) {
			return null;
		}

		NuxeoClient nuxeoClient = new NuxeoClient.Builder().url(url).authentication(login, pwd).readTimeout(24 * 3600)
				.connectTimeout(60).transactionTimeout(24 * 3600).connect();

		return nuxeoClient;
	}

}
