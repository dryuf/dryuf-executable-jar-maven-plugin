package net.dryuf.maven.plugin.executablejar;

import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;


/**
 * CSV to localization messages generator.
 */
@Mojo(name = "create-executable", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GeneratorMojo extends AbstractMojo
{
	@SneakyThrows
	@SuppressWarnings("unchecked")
	@Override
	public void			execute() throws MojoExecutionException, MojoFailureException
	{
		Generator generator = new Generator();
		generator.setClassLoader(buildClasspath(mavenProject.getCompileClasspathElements()));
		Configuration configuration = new Configuration();
		configuration.setHeader(noHeader ? null : header);
		configuration.setVmParams(vmParams);
		configuration.setSort(sort);
		configuration.setDefaultResourceConfig(defaultResourceConfig);
		configuration.setResourceConfigs(resourceConfigs);
		configuration.setExternalResourceConfigs(externalResourceConfigs);
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

	private ClassLoader buildClasspath(List<String> classpath)
	{
		URL[] urls = classpath.stream().map((file) -> {
			try {
				return new File(file).toURI().toURL();
			}
			catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		})
			.toArray(URL[]::new);
		return new URLClassLoader(urls);
	}

	@Parameter(required = true, defaultValue = "${project}", readonly = true)
	private MavenProject		mavenProject;

	/** Avoid adding script header. */
	@Parameter(required = false)
	protected boolean		noHeader = false;

	/** Script header. */
	@Parameter(required = false, defaultValue = "#!/usr/bin/env -S java")
	protected String		header;

	/** VM parameters. */
	@Parameter(required = false)
	protected String		vmParams;

	/** Sorts the entries by path and name. */
	@Parameter(required = false)
	protected boolean		sort;

	/** Default Resource configuration. */
	@Parameter(required = false)
	protected Configuration.ResourceConfig defaultResourceConfig = new Configuration.ResourceConfig();

	/** Resource configuration for specific file pattern. */
	@Parameter(required = false)
	protected List<Configuration.ResourceConfig> resourceConfigs = new ArrayList<>();

	/** Resource configuration stored in external JSON files. */
	@Parameter(required = false)
	protected List<String>		externalResourceConfigs;

	/** Input jar file. */
	@Parameter(required = true)
	protected File			input;

	/** Output jar file. */
	@Parameter(required = false)
	protected File			output;
}
