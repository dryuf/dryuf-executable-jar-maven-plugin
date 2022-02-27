package net.dryuf.maven.plugin.executablejar.it.remove;

import org.apache.commons.compress.archivers.zip.ResourceAlignmentExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipMethod;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;


/**
 * Tests generated result.
 */
public class ResultTest
{
	@Test
	public void testResult() throws Exception
	{
		try (BufferedReader exec = new BufferedReader(new InputStreamReader(
			new FileInputStream("target/dryuf-executable-jar-maven-plugin-test"),
			StandardCharsets.UTF_8))) {
			String line = exec.readLine();
		}

		try (
			InputStream inputStreamLow = new FileInputStream("target/dryuf-executable-jar-maven-plugin-test");
			ZipArchiveInputStream zipFile = new ZipArchiveInputStream(inputStreamLow)) {
			ZipArchiveEntry entry;
			while ((entry = zipFile.getNextZipEntry()) != null) {
				Assert.assertFalse(entry.isDirectory());
			}
		}
	}
}
