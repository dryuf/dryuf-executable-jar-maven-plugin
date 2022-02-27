package net.dryuf.maven.plugin.executablejar.it.external;

import org.apache.commons.compress.archivers.zip.ResourceAlignmentExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipMethod;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
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
			Assert.assertEquals("#!/usr/bin/env java -Xmx16m -jar", line);
		}

		ZipFile zipFile = new ZipFile(openZipFile(Paths.get("target/dryuf-executable-jar-maven-plugin-test")));
		testFile(zipFile, "file.align64", true, 64);
		testFile(zipFile, "file.align256", true, 256);
		testFile(zipFile, "file.align512", true, 512);
		testFile(zipFile, "file.compressed256", false, 256);
		testFile(zipFile, "file.compressed", false, 4);
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

	private SeekableByteChannel openZipFile(Path path) throws IOException
	{
		SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ);
		ByteBuffer buf = ByteBuffer.allocate(65536);
		channel.read(buf);
		final long offset;
		for (int start = 0; ; ++start) {
			if (start > buf.position()-4)
				throw new IOException("Cannot find PKZip header in first 64 KiB: path="+path);
			if (buf.get(start) == 'P' && buf.get(start+1) == 'K' && buf.get(start+2) == '\3' && buf.get(start+3) == '\4') {
				offset = start;
				break;
			}
		}
		return new SeekableByteChannel()
		{
			@Override
			public int read(ByteBuffer dst) throws IOException
			{
				return channel.read(dst);
			}

			@Override
			public int write(ByteBuffer src) throws IOException
			{
				return channel.write(src);
			}

			@Override
			public long position() throws IOException
			{
				return channel.position()-offset;
			}

			@Override
			public SeekableByteChannel position(long newPosition) throws IOException
			{
				if (newPosition < 0)
					throw new IllegalArgumentException("newPosition must be non-negative");
				channel.position(newPosition+offset);
				return this;
			}

			@Override
			public long size() throws IOException
			{
				return channel.size()-offset;
			}

			@Override
			public SeekableByteChannel truncate(long size) throws IOException
			{
				return channel.truncate(size+offset);
			}

			@Override
			public boolean isOpen()
			{
				return channel.isOpen();
			}

			@Override
			public void close() throws IOException
			{
				channel.close();
			}
		};
	}

}
