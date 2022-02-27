package net.dryuf.maven.plugin.executablejar.it.alignment;

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
			Assert.assertTrue(line.startsWith("PK\3\4"));
		}

		ZipFile zipFile = new ZipFile("target/dryuf-executable-jar-maven-plugin-test");
		testFile(zipFile, "file.align64", true, 64);
		testFile(zipFile, "file.align256", true, 256);
		testFile(zipFile, "file.align512", true, 512);
		testFile(zipFile, "file.compressed256", false, 256);
		testFile(zipFile, "file.compressed", false, null);
		testFile(zipFile, "file.stored", true, 16);
	}

	private void testFile(ZipFile zipFile, String name, boolean stored, Integer alignment) throws Exception
	{
		ZipArchiveEntry entry = zipFile.getEntry(name);
		Assert.assertNotNull("File does not exist in zip: file="+name, entry);
		Assert.assertEquals(stored, entry.getMethod() == ZipMethod.STORED.getCode());
		ResourceAlignmentExtraField alignmentField = (ResourceAlignmentExtraField)entry.getExtraField((ResourceAlignmentExtraField.ID));
		Assert.assertEquals(Optional.ofNullable(alignment), Optional.ofNullable(alignmentField).map(f -> (int) f.getAlignment()));
		if (alignment != null) {
			Assert.assertEquals(0, entry.getDataOffset()&(alignment-1));
		}
	}
}
