package net.dryuf.maven.plugin.executablejar;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;


/**
 * CSV to localization messages generator.
 */
@Mojo(name = "create-executable", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GeneratorMojo extends AbstractMojo
{
	@SuppressWarnings("unchecked")
	@Override
	public void			execute() throws MojoExecutionException, MojoFailureException
	{
		Generator generator = new Generator();
		Configuration configuration = new Configuration();
		configuration.setHeader(header);
		configuration.setVmParams(vmParams);
		configuration.setInputFile(input);
		configuration.setOutputFile(output);
		generator.setConfiguration(configuration);
		try {
			generator.execute();
		}
		catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	/** Script header. */
	@Parameter(required = false)
	protected String		header = "#!/usr/bin/env java";

	/** VM parameters. */
	@Parameter(required = false)
	protected String		vmParams;

	/** Input jar file. */
	@Parameter(required = true)
	protected File			input;

	/** Output jar file. */
	@Parameter(required = false)
	protected File			output;
}
