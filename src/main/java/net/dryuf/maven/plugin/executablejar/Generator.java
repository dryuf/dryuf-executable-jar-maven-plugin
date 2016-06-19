package net.dryuf.maven.plugin.executablejar;

import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;


/**
 * Main generator class
 */
public class Generator
{
	public void			execute() throws IOException
	{
		File outputFile = Optional.ofNullable(configuration.outputFile)
			.orElseGet(() -> {
				if (!FilenameUtils.getExtension(configuration.inputFile.toString()).equals("jar"))
					throw new IllegalArgumentException("Cannot infer output filename from input filename, not ending with .jar: "+configuration.inputFile);
				return new File(FilenameUtils.removeExtension(configuration.inputFile.toString()));
			});
		if (!outputFile.exists() || configuration.inputFile.lastModified() > outputFile.lastModified()) {
			try (
				InputStream inputStream = new FileInputStream(configuration.inputFile);
				OutputStream outputStream = new FileOutputStream(outputFile)
			) {
				processJar(inputStream, outputStream);
			}
			catch (Throwable ex) {
				outputFile.delete();
				throw ex;
			}
			outputFile.setExecutable(true);
		}
	}

	private void processJar(InputStream inputStream, OutputStream outputStream) throws IOException
	{
		outputStream.write(
			(configuration.getHeader() +
				Optional.ofNullable(configuration.vmParams).map(s -> " " + s).orElse("") +
				" -jar" +
				"\n"
			).getBytes(StandardCharsets.UTF_8)
		);
		IOUtils.copy(inputStream, outputStream);
	}

	@Setter
	protected Configuration		configuration;
}
