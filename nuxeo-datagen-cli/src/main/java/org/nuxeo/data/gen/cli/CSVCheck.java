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

public class CSVCheck {

	public static void main(String[] args) {

		Options options = new Options();
		options.addOption("i", "id", true, "ID Source file");
		options.addOption("c", "idColumn", true, "id Column");
		options.addOption("a", "account", true, "Account Source file");
		options.addOption("d", "accountColumn", true, "account Column");
		options.addOption("h", "help", false, "help");

		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
		}

		String idsfile = cmd.getOptionValue("i");
		String accountsfile = cmd.getOptionValue("a");
		int cid = Integer.parseInt(cmd.getOptionValue("c", "8"));
		int aid = Integer.parseInt(cmd.getOptionValue("d", "8"));

		if (cmd.hasOption('h')) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("CSVDedup", options);
			return;
		}

		if (idsfile == null || accountsfile == null) {
			System.err.println("ids and accounts need to be defined");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("CSVDedup", options);
			return;
		}

		System.err.println("Reading the ids");
		Set<String> ids = new KeyBuffer(50000000, 5000);
		long processedLines = 0;
		long skippedLines = 0;

		File csv = new File(idsfile);
		Scanner scanner = null;
		try {
			scanner = new Scanner(csv);
		} catch (Exception e) {
			System.err.println("Unable to open intput file " + e.getMessage());
			e.printStackTrace();
			return;
		}

		PrintWriter out = null;
		PrintWriter filtred = null;
		try {
			out = new PrintWriter(new FileWriter(new File("accounts-withoutIDCard.csv")));
			filtred = new PrintWriter(new FileWriter(new File("verified-accounts.csv")));
		} catch (IOException e) {
			System.err.println("Unable to open output file " + e.getMessage());
			e.printStackTrace();
			return;
		}

		System.out.println("Procesing " + csv.getAbsolutePath());
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			String[] parts = line.split(",");
			String id = parts[cid];
			id = id.substring(0, id.length() - 3);
			if (ids.contains(id)) {
				System.out.println("Found duplicate for key " + id);
				return;
			} else {
				ids.add(id);
			}

		}
		scanner.close();
		System.out.println("collected " + ids.size() + " keys");
		csv = new File(accountsfile);

		try {
			scanner = new Scanner(csv);
		} catch (Exception e) {
			System.err.println("Unable to open intput file " + e.getMessage());
			e.printStackTrace();
			return;
		}
		System.out.println("Procesing " + csv.getAbsolutePath());
		long missing = 0;
		long processed = 0;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			String[] parts = line.split(",");
			String account = parts[aid];
			String id = account.substring(0, account.length() - 3);
			processed++;
			if (!ids.contains(id)) {
				//System.out.println("Can not find id for account " + account);
				//out.println(account);
				out.println(line);				
				missing++;
			} else {
				filtred.append(line + "\n");
			}
			if (processed % 10000 == 0) {
				System.out.print("\r line: " + processed + " missing:" + missing);
			}
		}
		scanner.close();
		
		System.out.println("\nline processed: " + processed + " # accounts missing:" + missing);
		
		out.close();
		filtred.close();
	
	}
}
