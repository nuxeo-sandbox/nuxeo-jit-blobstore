package org.nuxeo.data.gen.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.nuxeo.importer.stream.jit.USStateHelper;

public class CSVBalance {

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
		int col = Integer.parseInt(cmd.getOptionValue("c", "6"));

		if (source == null) {
			System.err.println("Source and Destination need to be defined");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("CSVBalance", options);
			return;
		}

		if (cmd.hasOption('h')) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("CSVBalance", options);
			return;
		}

		long processedLines = 0;
		long east = 0;
		long west = 0;

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

			String[] parts = line.split(",");

			if (parts.length > col) {
				String state = parts[col].trim();
				String sCode = USStateHelper.getStateCode(state);
				if (USStateHelper.isEastern(sCode)) {
					east++;
				} else {
					west++;
				}
			} else {
				System.out.println("\nUnable to parse line " + processedLines);
				System.out.println("==>" + line);
			}

			if (processedLines % 50000 == 0) {
				long pEast = (100 * east) / (processedLines);
				long pWest = (100 * west) / (processedLines);
				System.out.printf("East %d %% - West %d %% \r", pEast, pWest);
			}
		}
		scanner.close();
		
		long pEast = (100 * east) / (processedLines);
		long pWest = (100 * west) / (processedLines);
		System.out.printf("\nEast %d %% - West %d %% ", pEast, pWest);

	}
}
