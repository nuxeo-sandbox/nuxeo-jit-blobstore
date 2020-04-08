package org.nuxeo.data.gen.out;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class FolderBatchWriterWithCmdCB extends AbstractFolderBatchWriter {

	protected ProcessBuilder pbuilder = new ProcessBuilder();

	protected final String[] cmd;

	protected List<String> stdOut;

	
	protected boolean cleanup = false;

	public FolderBatchWriterWithCmdCB(String folder, int batchSize, int total, String[] cmd, boolean cleanup) {
		super(folder, batchSize, total);
		this.cmd = cmd;
		stdOut = new ArrayList<String>();
		this.cleanup=cleanup;
	}

	public FolderBatchWriterWithCmdCB(String folder, int batchSize, int total, String shellCmd, boolean cleanup) {
		this(folder, batchSize, total, new String[] {"bash", "-c", shellCmd}, cleanup);		
	}

	protected String[] getCmd() {
		String[] c = new String[cmd.length];
		System.arraycopy(cmd, 0, c, 0, cmd.length);
		return c;
	}

	public List<String> getStdOut() {
		return stdOut;
	}

	@Override
	protected void batchCompledtedCB(int batch, String path) {

		String[] cmds = getCmd();
		for (int i = 0; i < cmds.length; i++) {
			cmds[i] = cmds[i].replace("%dir%", path);
		}

		pbuilder.command(cmds);		
		pbuilder.directory(new File(path));

		try {
			Process process = pbuilder.start();											
			String line = new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
	
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				System.out.println(" Execution failed");
			} else {
				stdOut.add(line);
				if (cleanup) {
					try {
						FileUtils.deleteDirectory(new File(path));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}					
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
