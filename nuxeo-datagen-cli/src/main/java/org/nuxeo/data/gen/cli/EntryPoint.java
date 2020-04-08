package org.nuxeo.data.gen.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.nuxeo.data.gen.meta.SequenceGenerator;
import org.nuxeo.data.gen.out.BlobWriter;
import org.nuxeo.data.gen.out.FolderWriter;
import org.nuxeo.data.gen.out.S3TMAWriter;
import org.nuxeo.data.gen.out.S3TMWriter;
import org.nuxeo.data.gen.out.S3Writer;
import org.nuxeo.data.gen.out.TmpWriter;
import org.nuxeo.data.gen.out.filter.JpegFilter;
import org.nuxeo.data.gen.out.filter.TiffFilter;
import org.nuxeo.data.gen.out.filter.WriteFilter;
import org.nuxeo.data.gen.pdf.itext.ITextNXBankStatementGenerator;
import org.nuxeo.data.gen.pdf.itext.ITextNXBankTemplateCreator;
import org.nuxeo.data.gen.pdf.itext.ITextNXBankTemplateCreator2;

public class EntryPoint {

	protected static LoggerContext initLogger() {
		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

		AppenderComponentBuilder console = builder.newAppender("stdout", "Console");
		builder.add(console);

		AppenderComponentBuilder file1 = builder.newAppender("metadata", "File");
		file1.addAttribute("fileName", "metadata.csv");
		builder.add(file1);

		AppenderComponentBuilder file2 = builder.newAppender("injector", "File");
		file2.addAttribute("fileName", "injector.log");
		builder.add(file2);

		// Use Async Logger
		RootLoggerComponentBuilder rootLogger = builder.newAsyncRootLogger(Level.INFO);
		//rootLogger.add(builder.newAppenderRef("stdout"));
		// rootLogger.add(builder.newAppenderRef("log"));
		builder.add(rootLogger);

		// Use Async Logger
		LoggerComponentBuilder logger1 = builder.newAsyncLogger("metadataLogger", Level.DEBUG);
		logger1.add(builder.newAppenderRef("metadata"));
		logger1.addAttribute("additivity", false);
		builder.add(logger1);

		// Use Async Logger
		LoggerComponentBuilder logger2 = builder.newAsyncLogger("importLogger", Level.DEBUG);
		logger2.add(builder.newAppenderRef("injector"));
		logger2.addAttribute("additivity", false);
		builder.add(logger2);
		
		LoggerComponentBuilder logger3 = builder.newAsyncLogger("cmdLogger", Level.INFO);
		logger3.add(builder.newAppenderRef("stdout"));
		logger3.add(builder.newAppenderRef("injector"));
		logger3.addAttribute("additivity", false);
		builder.add(logger3);
		

		return Configurator.initialize(builder.build());
	}

	public static void main(String[] args) {

		LoggerContext ctx = initLogger();

		Logger importLogger = ctx.getLogger("importLogger");
		Logger metadataLogger = ctx.getLogger("metadataLogger");
		Logger cmdLogger = ctx.getLogger("cmdLogger");		

		Options options = new Options();
		options.addOption("m", "mode", true, "define generation mode: id (default), metadata, pdf");
		options.addOption("t", "threads", true, "Number of threads");
		options.addOption("n", "nbDoc", true, "Number of Documents to generate");
		options.addOption("d", "months", true, "Number of months of statements to generate");
		options.addOption("o", "output", true, "generate and output PDF : mem (default), tmp, file:<path>, s3:<bucketName>, s3tm:<bucketName>, s3tma:<bucketName>");
		options.addOption("h", "help", false, "Help");
		options.addOption("s", "seed", true, "Seed");
		options.addOption("f", "filter", true, "filter to be applied: tiff, jpeg");
		options.addOption("aws_key", true, "AWS_ACCESS_KEY_ID");
		options.addOption("aws_secret", true, "AWS_SECRET_ACCESS_KEY");
		options.addOption("aws_session", true, "AWS_SESSION_TOKEN");
		
		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
		}

		int nbThreads = Integer.parseInt(cmd.getOptionValue('t', "10"));
		int nbDocs = Integer.parseInt(cmd.getOptionValue('n', "100000"));
		int nbMonths = Integer.parseInt(cmd.getOptionValue('d', "48"));

		long seed = Long.parseLong(cmd.getOptionValue('s', SequenceGenerator.DEFAULT_ACCOUNT_SEED+""));
				
		String modeStr = cmd.getOptionValue('m', "id");
		Injector.MODE mode = Injector.MODE.ID;
		try {
			mode = Injector.MODE.valueOf(modeStr.toUpperCase());
		} catch (Exception e) {
			System.err.println("Invalid mode : " + modeStr);
			return;
		}
						
		String out = cmd.getOptionValue('o', "mem");
		BlobWriter writer = null;
		if (TmpWriter.NAME.equalsIgnoreCase(out)) {
			importLogger.log(Level.INFO, "Inititialize Tmp Writer");
			writer = new TmpWriter();
		} else if (out.startsWith(FolderWriter.NAME)) {
			String folder = out.substring(FolderWriter.NAME.length() );
			importLogger.log(Level.INFO, "Inititialize Folder Writer in " + folder);
			writer = new FolderWriter(folder);
		} else if (out.startsWith(S3Writer.NAME)) {
			String bucketName = out.substring(S3Writer.NAME.length() );
			importLogger.log(Level.INFO, "Inititialize S3 Writer in bucket " + bucketName);

			String aws_key = cmd.getOptionValue("aws_key", null);
			String aws_secret = cmd.getOptionValue("aws_secret", null);
			String aws_session = cmd.getOptionValue("aws_session", null);
			writer = new S3Writer(bucketName, aws_key, aws_secret, aws_session);
		} else if (out.startsWith(S3TMWriter.NAME)) {
			String bucketName = out.substring(S3TMWriter.NAME.length());
			importLogger.log(Level.INFO, "Inititialize S3TM Writer in bucket " + bucketName);

			String aws_key = cmd.getOptionValue("aws_key", null);
			String aws_secret = cmd.getOptionValue("aws_secret", null);
			String aws_session = cmd.getOptionValue("aws_session", null);
			writer = new S3TMWriter(bucketName, aws_key, aws_secret, aws_session);
		} else if (out.startsWith(S3TMAWriter.NAME)) {
			String bucketName = out.substring(S3TMAWriter.NAME.length());
			importLogger.log(Level.INFO, "Inititialize S3TMA Writer in bucket " + bucketName);

			String aws_key = cmd.getOptionValue("aws_key", null);
			String aws_secret = cmd.getOptionValue("aws_secret", null);
			String aws_session = cmd.getOptionValue("aws_session", null);
			writer = new S3TMAWriter(bucketName, aws_key, aws_secret, aws_session);
		}

		if (writer!=null) {
			String filterName = cmd.getOptionValue('f', "");
			WriteFilter filter=null;
			if (TiffFilter.NAME.equalsIgnoreCase(filterName)) {
				filter = new TiffFilter();
			} else 	if (JpegFilter.NAME.equalsIgnoreCase(filterName)) {
				filter = new JpegFilter();
			}			
			if (filter!=null) {
				writer.setFilter(filter);
			}
		}
		
		if (cmd.hasOption('h')) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("DataGenCLI", options);
			return;
		}

		//ctx.getRootLogger().log(Level.INFO, "You Man");
		cmdLogger.log(Level.INFO,  "Selected Generation mode:" + mode.toString());
		if (writer==null) {
			cmdLogger.log(Level.INFO,  "Output Driver: mem");
		} else {
			cmdLogger.log(Level.INFO,  "Output Driver:" + writer.getClass().getSimpleName());	
		}		
		cmdLogger.log(Level.INFO, "Init Injector");
		cmdLogger.log(Level.INFO, "  Threads:" + nbThreads);
		cmdLogger.log(Level.INFO, "  nbDocs:" + nbDocs);
		cmdLogger.log(Level.INFO, "  nbMonths:" + nbMonths);
		
		try {
			runInjector(mode, seed, nbDocs, nbThreads, nbMonths, importLogger, metadataLogger, writer);
		} catch (Exception e) {
			System.err.println("Error while running Injector " + e);
			e.printStackTrace();
		}
		
		ctx.close();
	}

	protected static void runInjector(Injector.MODE mode, long seed, int total, int threads, int nbMonths, Logger importLogger, Logger metadataLogger,
			BlobWriter writer) throws Exception {

		// Data Generator
		ITextNXBankTemplateCreator templateGen = null;
		templateGen = new ITextNXBankTemplateCreator2();

		// Generate the template
		InputStream logo = EntryPoint.class.getResourceAsStream("/NxBank3.png");
		templateGen.init(logo);

		ByteArrayOutputStream templateOut = new ByteArrayOutputStream();
		templateGen.generate(templateOut);
		byte[] templateData = templateOut.toByteArray();

		// Init PDF generator
		ITextNXBankStatementGenerator gen = new ITextNXBankStatementGenerator();
		gen.init(new ByteArrayInputStream(templateData), templateGen.getKeys());
		gen.computeDigest = true;

		Injector injector = new Injector(mode, seed,  gen, total, threads, nbMonths, importLogger, metadataLogger);
		injector.setWriter(writer);
		injector.run();

	}
}
