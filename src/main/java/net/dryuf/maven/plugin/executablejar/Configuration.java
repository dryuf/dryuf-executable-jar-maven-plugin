package net.dryuf.maven.plugin.executablejar;

import lombok.Data;

import java.io.File;


/**
 * Plugin configuration.
 */
@Data
public class Configuration
{
	/** Script header. */
	protected String		header = "#!/usr/bin/env java";

	/** VM parameters. */
	protected String		vmParams;

	/** Input jar file. */
	protected File			inputFile;

	/** Output jar file. */
	protected File			outputFile;
}
