package org.nuxeo.data.gen.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class SBSample {

	protected static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("DataGenCLI", options);
	}

	public static void main(String[] args) {

		Options options = new Options();
		options.addOption("index", true, "csv index file for the snowball");
		options.addOption("source", true, "source file used to do sampling");
		options.addOption("col", true, "colum to use in the source file");
		options.addOption("sample", true, "modulo applied on source file lines");

		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
		}

		String sb = cmd.getOptionValue("index", "s3ls.csv");

		String source = cmd.getOptionValue("source");
		if (source ==null) {
			System.out.println("Source can not be null");
			printHelp(options);
		}

		Integer col = Integer.parseInt(cmd.getOptionValue("col", "0"));
		
		Integer modulo = Integer.parseInt(cmd.getOptionValue("sample", "1000"));
		
		Set<String> samples = collectSamples(source, col, modulo);
		
		checkSB(sb, samples);		
	}

	
	protected static Set<String> collectSamples(String fileName, int col, int modulo) {

		System.out.print("\nCollecting samples  on file " + fileName + "\n");
		Set<String> samples = new HashSet<String>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(fileName)));
			String line = reader.readLine();
			long count =new Random().nextInt(modulo);
			while (line != null) {
				count++;
				
				if (count%modulo==0) {
					String[] parts = line.split(",");
					String key = parts[col];
					samples.add(key);
					System.out.print("Collected sample # " + samples.size() + "\r");
				}
				
				line = reader.readLine();
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return samples;
	}

	protected static void checkSB(String fileName, Set<String> samples) {

		BufferedReader reader = null;
		List<String> found = new ArrayList<String>();
		
		System.out.print("\n\nScanning index to find sample values\n");
		
		try {
			reader = new BufferedReader(new FileReader(new File(fileName)));
			String line = reader.readLine();
			long l=0;
			while (line != null) {
				l++;
				if (l%5000==0) {
					System.out.printf("Scanning line %,d \r", l);
				}
				String[] parts = line.split(",");
				
				String key = parts[0];
				if (samples.contains(key)) {
					found.add(key);
				}
				
				line = reader.readLine();
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.printf("\n\n# of Samples      : %,d", samples.size());
		System.out.printf("\n# of Samples found: %,d", found.size());
		if (samples.size() == found.size()) {
			System.out.println("\nSB OK");	
		}

	}

}
