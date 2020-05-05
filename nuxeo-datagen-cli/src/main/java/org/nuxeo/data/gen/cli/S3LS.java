package org.nuxeo.data.gen.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class S3LS {
		
	public static void main(String[] args) {
		
		Options options = new Options();
		options.addOption("aws_session", true, "AWS_SESSION_TOKEN");
		options.addOption("aws_endpoint", true, "AWS_ENDPOINT");
		options.addOption("bucket", true, "bucket");
		
		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
		}
		
		String bucket = cmd.getOptionValue("bucket", "10b-benchmark-blobstore");
		String aws_endpoint = cmd.getOptionValue("aws_endpoint", null);

		S3Index idx = new S3Index(aws_endpoint, bucket);
		idx.listV1();
	}	
}
