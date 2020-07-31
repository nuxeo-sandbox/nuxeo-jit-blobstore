package org.nuxeo.data.gen.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.nuxeo.data.gen.meta.FormatUtils;
import org.nuxeo.data.gen.out.BlobWriter;
import org.nuxeo.data.gen.out.FolderWriter;
import org.nuxeo.data.gen.out.S3Writer;
import org.nuxeo.data.gen.pdf.StatementMeta;
import org.nuxeo.data.gen.pdf.itext.ITextIDGenerator;
import org.nuxeo.data.gen.pdf.itext.ITextIDTemplateCreator;

import com.google.common.io.Files;

public class CSVAccount2IDCards {

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("a", "account", true, "Account Source file");
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

	protected static class PdfGenTask implements Runnable {

		protected final List<String> csv;
		
		protected final ITextIDGenerator gen;;
		
		protected final BlobWriter out;
		
		protected final Logger logger;
		
		protected final int STEP=100;
		
		public PdfGenTask(List<String> input, ITextIDGenerator gen, BlobWriter out, Logger logger) {		
			csv = new ArrayList<>(input);
			this.gen = gen;
			this.out=out;
			this.logger=logger;
		}
		
		@Override
		public void run() {

			String[] pdfMeta= new String[7];				
			List<String> newLine = new ArrayList<>();
			
			int count =0;
			
			for (String line : csv) {
				
				String[] parts = line.split(",");

				pdfMeta[0]=FormatUtils.pad(parts[3],41, false);
				pdfMeta[1]=FormatUtils.pad(parts[4],20, false);
				pdfMeta[2]=FormatUtils.pad(parts[5],20, false);
				pdfMeta[3]=FormatUtils.pad(parts[6],20, false);
				//pdfKeys[4]=parts[3];
				pdfMeta[5]=FormatUtils.pad(parts[8],20, false);
				
				ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();

				StatementMeta meta;
				try {
					meta = gen.generate(pdfOut, pdfMeta);
					newLine.clear();
					newLine.add(meta.getDigest());
					newLine.add(meta.getFileName());			
					newLine.add(meta.getFileSize()+"");
			
					for (int i =3; i <=8; i++) {
						newLine.add(parts[i]);					
					}
					
					if (out!=null) {
						out.write(pdfOut.toByteArray(), meta);
					}
					//store(out, meta.getDigest(), pdfOut.toByteArray());
					logger.info(String.join(",", newLine));
					
				} catch (Exception e) {
					e.printStackTrace();				
				}
				count++;
				if (count%STEP==0) {
					counter.addAndGet(STEP);
				}
			}
			
		}
		
	}
	
	protected static AtomicLong counter = new AtomicLong();
	protected static ThreadPoolExecutor executor;
	protected static long t0;
	
	public static void process(Options options, CommandLine cmd) throws Exception {

		LoggerContext ctx = LoggerHelper.initLoggingContext();
	
		Logger metadataLogger = ctx.getLogger(LoggerHelper.METADATA);
		Logger cmdLogger = ctx.getLogger(LoggerHelper.CMD);

		
		String accountsfile = cmd.getOptionValue("a");
		String outFolder = cmd.getOptionValue("o");
		String picFolder = cmd.getOptionValue("p");
		
		BlobWriter out=null;
		if (outFolder!=null) {
			if (outFolder.startsWith("s3:")) {
				out = new S3Writer(outFolder);
			} else {
				out = new FolderWriter(outFolder);		
			}
		}			
		
		if (cmd.hasOption('h')) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("CSVDedup", options);
			return;
		}

		if (accountsfile == null) {
			System.err.println("source accounts need to be defined");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("CSVDedup", options);
			return;
		}

		long processedLines = 0;

		System.out.println("Init template engine");	
		ITextIDTemplateCreator templateGen = new ITextIDTemplateCreator();		
		InputStream bg = ITextIDTemplateCreator.class.getResourceAsStream("/id-back-2.jpeg");
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

		cmdLogger.info("Procesing " + csv.getAbsolutePath());
		
		t0 = System.currentTimeMillis();
		
		int batchSize = 1000;
		int nbThreads=10;
		
		List<String> batch = new ArrayList<>();

		executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nbThreads);
		executor.prestartAllCoreThreads();
	
		
		while (scanner.hasNextLine()) {

			processedLines++;
			String line = scanner.nextLine();
		
			batch.add(line);
			
			if (processedLines%batchSize==0) {
				executor.submit(new PdfGenTask(batch, gen, out, metadataLogger));
				batch.clear();
			}
			
			while (executor.getQueue().size() > 50) {
				info(cmdLogger);
				Thread.sleep(5000);
				cmdLogger.info(".");				
			}
			
		}
		scanner.close();	
				
		executor.shutdown();
		boolean finished = false;
		long pauseTimeS = 2;

		while (!finished) {
			info(cmdLogger);
			try {
				finished = executor.awaitTermination(pauseTimeS, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected static void info(Logger cmdLogger) {
		long t1 = System.currentTimeMillis();
		int threads = executor.getActiveCount();

		long elapsed = (t1 - t0);
		Long throughput = Math.round(counter.get() * 1.0 / (elapsed / 1000));

		cmdLogger.info(" ------- " );
		cmdLogger.info(" Lines processed:" + counter.get());
		cmdLogger.info(" Throughput:" + throughput + " d/s using " + threads + " threads");
		cmdLogger.info(" Queue size: " + executor.getQueue().size());
		cmdLogger.info(" Tasks count: " + executor.getTaskCount());
	}
	
	protected static void store(String rootFolder, String digest, byte[] pdf) throws Exception{
		
		if (rootFolder=="" | rootFolder==null) {
			//System.out.println("fake write for " + digest);
			return;
		}
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
