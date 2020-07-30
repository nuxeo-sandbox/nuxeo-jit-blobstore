package org.nuxeo.data.gen.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.nuxeo.data.gen.meta.FormatUtils;
import org.nuxeo.data.gen.pdf.StatementMeta;
import org.nuxeo.data.gen.pdf.itext.ITextIDGenerator;
import org.nuxeo.data.gen.pdf.itext.ITextIDTemplateCreator;

import com.google.common.io.Files;

public class CSVAccount2IDCards {

	

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("a", "account", true, "Account Source file");
		options.addOption("i", "id", true, "IDCard generated csv file");
		options.addOption("o", "out", true, "IDCard generated pdf files");
		
		options.addOption("p", "picture", true, "picture directory");
		
		options.addOption("h", "help", false, "help");

		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
		}
		
		try {
			process(options,cmd);
		} catch (Exception e) {
			e.printStackTrace();
		}
		

	}

	public static void process(Options options, CommandLine cmd) throws Exception {

		
		String idsfile = cmd.getOptionValue("i");
		String accountsfile = cmd.getOptionValue("a");
		String outFolder = cmd.getOptionValue("o");
		String picFolder = cmd.getOptionValue("p");
		
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

		long processedLines = 0;

		System.out.println("Init template engine");	
		ITextIDTemplateCreator templateGen = new ITextIDTemplateCreator();		
		InputStream bg = ITextIDTemplateCreator.class.getResourceAsStream("/id-back.jpeg");
		templateGen.init(bg);
		String[] keys = templateGen.getKeys();		
		ByteArrayOutputStream templateOut = new ByteArrayOutputStream();
		templateGen.generate(templateOut);
		byte[] templateData =  templateOut.toByteArray();

		
		ITextIDGenerator gen = new ITextIDGenerator();
		gen.init(new ByteArrayInputStream(templateData), keys);
		gen.computeDigest = true;		
		if (picFolder!=null && !picFolder.isBlank()) {
			gen.setPictureFolder(Path.of(picFolder));
		} else {
			gen.setPicture(ITextIDTemplateCreator.class.getResourceAsStream("/jexo.jpeg"));
		}
		
		File csv = new File(accountsfile);
		Scanner scanner = null;
		try {
			scanner = new Scanner(csv);
		} catch (Exception e) {
			System.err.println("Unable to open intput file " + e.getMessage());
			e.printStackTrace();
			return;
		}

		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(new File(idsfile)));
		} catch (IOException e) {
			System.err.println("Unable to open output file " + e.getMessage());
			e.printStackTrace();
			return;
		}

		System.out.println("Procesing " + csv.getAbsolutePath());
		String[] pdfMeta= new String[7];				
		List<String> newLine = new ArrayList<>();
		
		long t0 = System.currentTimeMillis();
		while (scanner.hasNextLine()) {

			processedLines++;
			String line = scanner.nextLine();
			String[] parts = line.split(",");

			pdfMeta[0]=FormatUtils.pad(parts[3],41, false);
			pdfMeta[1]=FormatUtils.pad(parts[4],20, false);
			pdfMeta[2]=FormatUtils.pad(parts[5],20, false);
			pdfMeta[3]=FormatUtils.pad(parts[6],20, false);
			//pdfKeys[4]=parts[3];
			pdfMeta[5]=FormatUtils.pad(parts[8],20, false);
			
			ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();

			if (processedLines %10000==0) {
				System.out.println("Generated " + processedLines + " id cards");
				double tp = (1000*processedLines)/ (System.currentTimeMillis()-t0);
				System.out.println("Throughput: " + tp + " pdf/s");		
			}
			StatementMeta meta = gen.generate(pdfOut, pdfMeta);
			newLine.clear();
			newLine.add(meta.getDigest());
			newLine.add(meta.getFileName());			
			newLine.add(meta.getFileSize()+"");
	
			for (int i =3; i <=8; i++) {
				newLine.add(parts[i]);					
			}
			
			store(outFolder, meta.getDigest(), pdfOut.toByteArray());
			
			//File pdfFile = new File (outFolder, meta.getDigest());
			//Files.write(pdfOut.toByteArray(), pdfFile);
			
			out.println(String.join(",", newLine));

		}
		scanner.close();			
		out.close();
	
	}
	
	protected static void store(String rootFolder, String digest, byte[] pdf) throws Exception{
		
		String prefix = digest.substring(0,2);
		File folder = new File(rootFolder, prefix);
		if (!folder.exists()) {
			folder.mkdir();
			folder = new File(rootFolder, prefix);
		}
		File pdfFile = new File (folder, digest);
		Files.write(pdf, pdfFile);
		
	}
}
