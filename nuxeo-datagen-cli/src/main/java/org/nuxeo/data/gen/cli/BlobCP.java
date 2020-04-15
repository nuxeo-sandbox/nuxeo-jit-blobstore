package org.nuxeo.data.gen.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.io.Files;

public class BlobCP {

	public static void main(String[] args) {

		Options options = new Options();
		options.addOption("s", "souce", true, "Source folder");
		options.addOption("o", "output", true, "Dest folder");
		options.addOption("d", "depth", true, "depth");
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
		String dest = cmd.getOptionValue("o");
		int depth = Integer.parseInt(cmd.getOptionValue("d", "2"));
		
		if (source ==null || dest == null) {
			System.err.println("Source and Destination need to be defined");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("BlobCP", options);
			return;
		}
		
		if (cmd.hasOption('h')) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("BlobCP", options);
			return;
		}
		
		for (File f : new File(source).listFiles()) {
			StringBuffer sb = new StringBuffer(dest);
			String name = f.getName();
			for (int i = 0; i < depth; i++) {				
				sb.append("/");
				sb.append(name.substring(i*2, (i+1)*2));
			}
			try {
				new File(sb.toString()).mkdirs();
			} catch (Exception e) {
				// whatever
			}
			sb.append("/");
			sb.append(f.getName());
			try {
				Files.copy(f, new File(sb.toString()));
			} catch (IOException e) {
				System.err.println("Error while copying file");
				e.printStackTrace();
			}			
		}
	}

}
