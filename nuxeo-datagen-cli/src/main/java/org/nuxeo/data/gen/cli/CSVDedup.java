package org.nuxeo.data.gen.cli;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CSVDedup {

	public static void main(String[] args) {

		Options options = new Options();
		options.addOption("s", "souce", true, "Source file");
		options.addOption("o", "output", true, "Dest file");
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

		String sourceOption = cmd.getOptionValue("s");
		String dest = cmd.getOptionValue("o");
		int col = Integer.parseInt(cmd.getOptionValue("c", "0"));

		if (sourceOption == null) {
			System.err.println("Source and Destination need to be defined");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("CSVDedup", options);
			return;
		}

		String[] sources = sourceOption.split(",");

		if (dest == null) {
			dest = sources[0] + ".debup";
		}

		if (cmd.hasOption('h')) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("CSVDedup", options);
			return;
		}

		PrintWriter out = null;
		PrintWriter dup = null;

		try {
			out = new PrintWriter(new FileWriter(new File(dest)));
			dup = new PrintWriter(new FileWriter(new File(dest + ".dup")));
		} catch (IOException e) {
			System.err.println("Unable to open output file " + e.getMessage());
			e.printStackTrace();
			return;
		}

		Set<String> ids = new KeyBuffer(50000000, 5000);

		long processedLines = 0;
		long skippedLines = 0;

		for (String source : sources) {

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

			long fProcessed = 0;
			long fSkipped = 0;

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();

				processedLines++;
				fProcessed++;

				if (fProcessed % 10000 == 0) {
					System.out.printf(" file %s line %,d \r", source, fProcessed);
				}

				String[] parts = line.split(",");

				if (parts.length > col) {
					String key = parts[col].trim();
					key = key.substring(0, key.length() - 3);
					if (!ids.contains(key)) {
						out.write(line + "\n");
						ids.add(key);
					} else {
						// System.out.println("Skip line " + fline + " :" + key);
						dup.write(key + "\n");
						skippedLines++;
						fSkipped++;
					}
				} else {
					System.out.println("\nUnable to parse line " + fProcessed);
					System.out.println("==>" + line);
				}
			}

			scanner.close();
			System.out.printf("\n\n skiped %,d / %,d on file %s ", fSkipped, fProcessed, source);
		}
		out.close();
		dup.close();
		System.out.printf("Finished procesing skiped %,d / %,d on %d files", skippedLines, processedLines,
				sources.length);
	}

}
