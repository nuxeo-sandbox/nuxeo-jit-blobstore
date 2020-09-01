package org.nuxeo.data.gen.cli;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class StateCounter {

	public static void main(String[] args) {

		Options options = new Options();
		options.addOption("s", "souce", true, "Source file");
		options.addOption("c", "column", true, "dedup Column");
		options.addOption("h", "help", false, "help");

		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
		}

		String source = cmd.getOptionValue("s");
		int col = Integer.parseInt(cmd.getOptionValue("c", "5"));

		if (source == null) {
			System.err.println("Source needs to be defined");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("StateCounter", options);
			return;
		}


		if (cmd.hasOption('h')) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("CSVDedup", options);
			return;
		}

		Map<String, AtomicInteger> counters = new HashMap<>();


		long processedLines = 0;


			File csv = new File(source);
			if (!csv.exists()) {
				System.err.println("Source file not found " + source);
				return;
			}
			Scanner scanner = null;
			try {
				scanner = new Scanner(csv);
			} catch (Exception e) {
				System.err.println("Unable to open intput file " + e.getMessage());
				e.printStackTrace();
				return;
			}

			System.out.println("Procesing " + csv.getAbsolutePath());

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();

				processedLines++;

				if (processedLines % 10000 == 0) {
					System.out.printf(" file %s line %,d \r", source, processedLines);
				}

				String[] parts = line.split(",");

				if (parts.length > col) {
					String key = parts[col].trim();
					
					if (counters.containsKey(key)) {
						counters.get(key).incrementAndGet();
					} else {
						counters.put(key, new AtomicInteger(1));
					}
										
				} else {
					System.out.println("\nUnable to parse line " + processedLines);
					System.out.println("==>" + line);
				}
			}

			scanner.close();
			
			for (String state: counters.keySet()) {
				System.out.println(state + ":" + counters.get(state).intValue());
			}
	}

}
