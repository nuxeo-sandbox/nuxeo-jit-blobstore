package org.nuxeo.data.gen.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class S3Stats {
		
	public static void main(String[] args) {
		
		Options options = new Options();
		options.addOption("csv", true, "csv file to read");
		
		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
		}
		
		String csv = cmd.getOptionValue("csv", "s3ls.csv");		
		
		computeStats(csv);
	}	
	
	protected static void computeStats(String fileName) {
		
		long s3Entries=0;
		long s3Archives=0;
		long totalEntries=0;		
		long totalSize=0;
		
		BufferedReader reader=null;
		
		try {
			reader = new BufferedReader(new FileReader(new File(fileName)));
			String line = reader.readLine();
			String lastArchive="";
			while (line !=null) {
				String[] parts = line.split(",");
				
				totalEntries++;
				
				if (parts[1].equals("")) {
					lastArchive="";
					s3Entries++;
				}	
				else if (!parts[1].equals(lastArchive)) {
						s3Archives++;
						s3Entries++;
						lastArchive=parts[1];
				} 
				totalSize += Long.parseLong(parts[2]);
				line = reader.readLine();				
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		} 

		System.out.printf("\nNumber of S3 entries  : %,d", s3Entries);
		System.out.printf("\n including S3 archives: %,d", s3Archives);
		System.out.printf("\nTotale files          : %,d",totalEntries);
		System.out.printf("\nTotale storage (GB)   : %,d", totalSize / (1024*1024*1024));
		
	}
	
}
