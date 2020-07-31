package org.nuxeo.data.gen.cli;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public class LoggerHelper {


	public static final String IMPORT = "importLogger";
	
	public static final String METADATA = "metadataLogger";
	
	public static final String CMD = "cmdLogger";
	
	public static LoggerContext initLoggingContext() {
		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

		AppenderComponentBuilder console = builder.newAppender("stdout", "Console");
		builder.add(console);

		AppenderComponentBuilder file1 = builder.newAppender("metadata", "File");
		file1.addAttribute("fileName", "metadata.csv");
		file1.addAttribute("filePattern", "metadata-%d{MM-dd-yy--hh:mm}-%i.csv");
		ComponentBuilder rolloverPolicy = builder.newComponent("Policies")
		        .addComponent(builder.newComponent("OnStartupTriggeringPolicy"));
		file1.addComponent(rolloverPolicy);	

		builder.add(file1);

		AppenderComponentBuilder file2 = builder.newAppender("injector", "RollingFile");
		file2.addAttribute("fileName", "injector.log");
		file2.addAttribute("filePattern", "injector-%d{MM-dd-yy--hh:mm}-%i.log");
		LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout").addAttribute("pattern","%d [%t] %-5level: %msg%n");
		file2.add(layoutBuilder);
        
		ComponentBuilder rolloverPolicy2 = builder.newComponent("Policies")
		        .addComponent(builder.newComponent("OnStartupTriggeringPolicy"));
		file2.addComponent(rolloverPolicy2);	
		
		builder.add(file2);

		// Use Async Logger
		RootLoggerComponentBuilder rootLogger = builder.newAsyncRootLogger(Level.INFO);
		// rootLogger.add(builder.newAppenderRef("stdout"));
		// rootLogger.add(builder.newAppenderRef("log"));
		builder.add(rootLogger);

		// Use Async Logger
		LoggerComponentBuilder logger1 = builder.newAsyncLogger(METADATA, Level.DEBUG);
		logger1.add(builder.newAppenderRef("metadata"));
		logger1.addAttribute("additivity", false);
		builder.add(logger1);

		// Use Async Logger
		LoggerComponentBuilder logger2 = builder.newAsyncLogger(IMPORT, Level.DEBUG);
		logger2.add(builder.newAppenderRef("injector"));
		logger2.addAttribute("additivity", false);
		builder.add(logger2);

		LoggerComponentBuilder logger3 = builder.newAsyncLogger(CMD, Level.INFO);
		logger3.add(builder.newAppenderRef("stdout"));
		logger3.add(builder.newAppenderRef("injector"));
		logger3.addAttribute("additivity", false);
		builder.add(logger3);

		return Configurator.initialize(builder.build());
	}

}
