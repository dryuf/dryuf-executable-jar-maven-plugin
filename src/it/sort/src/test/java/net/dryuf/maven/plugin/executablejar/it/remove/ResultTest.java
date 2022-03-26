package net.dryuf.maven.plugin.executablejar.it.sort;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Tests generated result.
 */
public class ResultTest
{
	@Test
	public void testResult() throws Exception
	{
		try (BufferedReader exec = new BufferedReader(new InputStreamReader(
			new FileInputStream("target/output.jar"),
			StandardCharsets.UTF_8))) {
			String line = exec.readLine();
		}

		List<String> entries = new ArrayList<>();
		try (
			InputStream inputStreamLow = new FileInputStream("target/output.jar");
			ZipArchiveInputStream zipFile = new ZipArchiveInputStream(inputStreamLow)) {
			ZipArchiveEntry entry;
			while ((entry = zipFile.getNextZipEntry()) != null) {
				entries.add(entry.getName());
			}
		}
		entries = entries.stream().filter((name) -> name.startsWith("a") || name.startsWith("b") || name.startsWith("t"))
				.collect(Collectors.toList());
		Assert.assertEquals(Arrays.asList(
						"a/",
						"a/first.txt",
						"a/sub/",
						"a/sub/more.txt",
						"a/subcont.txt",
						"b/",
						"b/sub.txt",
						"b$subclass.txt",
						"top.txt"
				),
				entries);
	}
}
