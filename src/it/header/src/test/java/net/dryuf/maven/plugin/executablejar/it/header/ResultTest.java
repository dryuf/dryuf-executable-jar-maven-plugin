package net.dryuf.maven.plugin.executablejar.it.header;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


/**
 * Tests generated result.
 */
public class ResultTest
{
	@Test
	public void testResult() throws IOException
	{
		try (BufferedReader exec = new BufferedReader(new InputStreamReader(
			new FileInputStream("target/dryuf-executable-jar-maven-plugin-test"),
			StandardCharsets.UTF_8))) {
			String line = exec.readLine();
			Assert.assertEquals("#!/usr/bin/env -S java -Xmx16m -jar", line);
		}
	}
}
