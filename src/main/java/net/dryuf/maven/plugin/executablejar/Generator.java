package net.dryuf.maven.plugin.executablejar;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import lombok.Setter;
import net.dryuf.maven.plugin.executablejar.concurrent.ResultSerializingExecutor;
import net.dryuf.maven.plugin.executablejar.io.PathMatcherUtil;
import net.dryuf.maven.plugin.executablejar.io.StringPathComparator;
import org.apache.commons.compress.archivers.zip.ResourceAlignmentExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipMethod;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Main generator class.
 */
public class Generator
{
	public void			execute() throws IOException
	{
		buildResourceStructures();

		File outputFile = Optional.ofNullable(configuration.outputFile)
			.orElseGet(() -> {
				if (!FilenameUtils.getExtension(configuration.inputFile.toString()).equals("jar"))
					throw new IllegalArgumentException("Cannot infer output filename from input filename, not ending with .jar: "+configuration.inputFile);
				return new File(FilenameUtils.removeExtension(configuration.inputFile.toString()));
			});
		if (!outputFile.exists() || configuration.inputFile.lastModified() > outputFile.lastModified()) {
			try (
				ZipFile inputZip = new ZipFile(configuration.inputFile);
				OutputStream outputStream = new FileOutputStream(outputFile)
			) {
				processJar(inputZip, outputStream);
				outputFile.setExecutable(true);
			}
			catch (Throwable ex) {
				outputFile.delete();
				throw ex;
			}
		}
	}

	private void buildResourceStructures()
	{
		resourceConfigs = new LinkedHashMap<>();
		Optional.ofNullable(configuration.resourceConfigs).orElse(Collections.emptyList())
			.forEach((Configuration.ResourceConfig rc) -> {
				try {
					processResourceConfig(resourceConfigs, rc);
				}
				catch (Exception ex) {
					throw new IllegalArgumentException("Failed to process entry: "+rc.getPattern()+" : "+ex, ex);
				}
			});
		if ((defaultResourceConfig = configuration.defaultResourceConfig) == null) {
			defaultResourceConfig = new Configuration.ResourceConfig();
		}

		Optional.ofNullable(configuration.externalResourceConfigs).orElse(Collections.emptyList()).stream()
			.forEach((String name) -> {
				try (InputStream stream = openResource(name)) {
					Configuration.ExternalResourceConfig erc = objectMapper.readValue(stream, Configuration.ExternalResourceConfig.class);
					Optional.ofNullable(erc.resourceConfigs).orElse(Collections.emptyList())
						.forEach((Configuration.ResourceConfig rc) -> {
							try {
								processResourceConfig(resourceConfigs, rc);
							}
							catch (Exception ex) {
								throw new RuntimeException("Failed to process entry: file="+name+" pattern="+rc.getPattern()+" : "+ex, ex);
							}
						});
					enrichDefaultResourceConfig(defaultResourceConfig, erc.defaultResourceConfig);
				}
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});

		enrichDefaultResourceConfig(defaultResourceConfig, DEFAULT_RESOURCE_CONFIG);
	}

	private void enrichDefaultResourceConfig(Configuration.ResourceConfig rc, Configuration.ResourceConfig source)
	{
		if (defaultResourceConfig.getRemove() == null)
			defaultResourceConfig.setRemove(source.getRemove());
		if (defaultResourceConfig.getMinimalCompress() == null)
			defaultResourceConfig.setMinimalCompress(source.getMinimalCompress());
		if (defaultResourceConfig.getKeepAlignment() == null)
			defaultResourceConfig.setKeepAlignment(source.getKeepAlignment());
		if (defaultResourceConfig.getCompressedAlignment() == null)
			defaultResourceConfig.setCompressedAlignment(source.getCompressedAlignment());
		if (defaultResourceConfig.getStoredAlignment() == null)
			defaultResourceConfig.setStoredAlignment(source.getStoredAlignment());
	}

	private void processJar(ZipFile inputZip, OutputStream outputStreamLow) throws IOException
	{
		final AtomicInteger maxAlignment = new AtomicInteger();
		try (ResultSerializingExecutor itemsExecutor = new ResultSerializingExecutor()) {
			Iterators.forEnumeration(inputZip.getEntriesInPhysicalOrder()).forEachRemaining((ZipArchiveEntry zipEntry) -> {
				FileEntry entry = new FileEntry();
				if (zipEntry.getCompressedSize() >= Integer.MAX_VALUE) {
					throw new UnsupportedOperationException("Input file entry too big: file="+zipEntry.getName()+" size="+zipEntry.getCompressedSize());
				}
				entry.zipEntry = zipEntry;
				itemsExecutor.submit(() -> processEntry(entry))
					.thenAccept((entry0) -> {
						if (entry0 != null)
							maxAlignment.updateAndGet(old -> Math.max(old, entry0.alignment));
					});
			});
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		if (configuration.getHeader() != null && !configuration.getHeader().isEmpty()) {
			byte[] header = (configuration.getHeader()+
				Optional.ofNullable(configuration.vmParams).map(s -> " "+s).orElse("")+
				" -jar"+
				"\n"
			).getBytes(StandardCharsets.UTF_8);
			outputStreamLow.write(header);
			outputStreamLow.write(StringUtils.repeat("\n", -header.length&(Math.max(maxAlignment.get(), 1)-1)).getBytes(StandardCharsets.UTF_8));
		}

		try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(outputStreamLow)) {
			AtomicReference<IOException> mainEx = new AtomicReference<>();
			ExecutorService writerExecutor = Executors.newSingleThreadExecutor();
			try (ResultSerializingExecutor itemsExecutor = new ResultSerializingExecutor()) {
				Iterator<ZipArchiveEntry> entries = Iterators.forEnumeration(inputZip.getEntriesInPhysicalOrder());
				if (configuration.isSort()) {
					entries = Streams.stream(entries)
							.sorted(Comparator.comparing(ZipArchiveEntry::getName, StringPathComparator.INSTANCE))
							.collect(ImmutableList.toImmutableList()).iterator();
				}
				entries.forEachRemaining((ZipArchiveEntry zipEntry) -> {
					FileEntry entry = new FileEntry();
					if (zipEntry.getCompressedSize() >= Integer.MAX_VALUE) {
						throw new UnsupportedOperationException("Input file entry too big: file="+zipEntry.getName()+" size="+zipEntry.getCompressedSize());
					}
					entry.zipEntry = zipEntry;
					itemsExecutor.submit(() ->processEntry(entry))
						.thenAccept((entry0) -> {
							if (entry0 != null)
								writeEntry(inputZip, outputStream, entry0);
						})
						.exceptionally((Throwable ex) -> {
							if (mainEx.get() == null) {
								if (mainEx.compareAndSet(null, new IOException("Failed to process jar", ex)))
									return null;
							}
							mainEx.get().addSuppressed(ex);
							return null;
						});
				});
				itemsExecutor.close();
				writerExecutor.shutdown();
				while (!writerExecutor.isTerminated()) {
					writerExecutor.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
				}
				if (mainEx.get() != null)
					throw mainEx.get();
			}
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private FileEntry processEntry(FileEntry entry)
	{
		ZipArchiveEntry zipEntry = entry.zipEntry;
		Path path = Paths.get(zipEntry.getName());
		Configuration.FileType fileType = getZipFileType(zipEntry);

		Configuration.ResourceConfig rcConfig = new Configuration.ResourceConfig();
		rcConfig.setRemove(searchResourceConfig(
			resourceConfigs,
			path,
			fileType,
			Configuration.ResourceConfig::getRemove
		)
			.findFirst()
			.orElseGet(() -> defaultResourceConfig.getRemove()));
		rcConfig.setMinimalCompress(searchResourceConfig(
			resourceConfigs,
			path,
			fileType,
			Configuration.ResourceConfig::getMinimalCompress
		)
			.findFirst()
			.orElseGet(() -> defaultResourceConfig.getMinimalCompress()));
		rcConfig.setKeepAlignment(searchResourceConfig(
			resourceConfigs,
			path,
			fileType,
			Configuration.ResourceConfig::getKeepAlignment
		)
			.findFirst()
			.orElseGet(() -> defaultResourceConfig.getKeepAlignment()));
		rcConfig.setCompressedAlignment(searchResourceConfig(
			resourceConfigs,
			path,
			fileType,
			Configuration.ResourceConfig::getCompressedAlignment
		)
			.filter(v -> v != 0)
			.findFirst()
			.orElseGet(() -> defaultResourceConfig.getCompressedAlignment()));
		rcConfig.setStoredAlignment(searchResourceConfig(
			resourceConfigs,
			path,
			fileType,
			Configuration.ResourceConfig::getStoredAlignment
		)
			.filter(v -> v != 0)
			.findFirst()
			.orElseGet(() -> defaultResourceConfig.getStoredAlignment()));

		if (rcConfig.getRemove()) {
			return null;
		}

		if (getCompressionRatio(zipEntry.getCompressedSize(), zipEntry.getSize()) < rcConfig.getMinimalCompress()) {
			entry.doStore = true;
		}
		else if (zipEntry.getMethod() == ZipMethod.STORED.getCode()) {
			entry.doStore = true;
		}

		ResourceAlignmentExtraField alignmentField = (ResourceAlignmentExtraField) entry.zipEntry.getExtraField(ResourceAlignmentExtraField.ID);
		final int alignment;
		if (alignmentField != null && rcConfig.getKeepAlignment()) {
			alignment = alignmentField.getAlignment();
		}
		else {
			alignment = entry.doStore ?
				rcConfig.getStoredAlignment() : rcConfig.getCompressedAlignment();
			if (alignment <= 1) {
				if (alignmentField != null)
					zipEntry.removeExtraField(ResourceAlignmentExtraField.ID);
			}
		}
		if ((alignment&(alignment-1)) != 0) {
			throw new IllegalArgumentException("Invalid alignment: file="+zipEntry.getName()+" alignment="+alignment);
		}
		entry.alignment = alignment;

		return entry;
	}

	private void writeEntry(ZipFile inputZip, ZipArchiveOutputStream outputStream, FileEntry entry)
	{
		ZipArchiveEntry zipEntry = entry.zipEntry;
		try {
			final InputStream rawInput;
			if (entry.doStore) {
				rawInput = inputZip.getInputStream(zipEntry);
				zipEntry.setMethod(ZipMethod.STORED.getCode());
			}
			else {
				rawInput = inputZip.getRawInputStream(zipEntry);
			}
			if (entry.alignment > 1) {
				zipEntry.setAlignment(entry.alignment);
				ResourceAlignmentExtraField raf = new ResourceAlignmentExtraField(entry.alignment);
				zipEntry.addExtraField(raf);
			}
			outputStream.addRawArchiveEntry(zipEntry, rawInput);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private int getCompressionRatio(long compressed, long uncompressed)
	{
		return (int) (uncompressed != 0 ? Math.multiplyExact(100, (uncompressed-compressed))/uncompressed : 0);
	}

	private void processResourceConfig(Map<PathMatcher, Configuration.ResourceConfig> resourceConfigs, Configuration.ResourceConfig rc)
	{
		if (rc.getCompressedAlignment() != null && (rc.getCompressedAlignment()&(rc.getCompressedAlignment())-1) != 0) {
			throw new IllegalArgumentException("Invalid compressedAlignment: "+rc.getCompressedAlignment());
		}
		if (rc.getStoredAlignment() != null && (rc.getStoredAlignment()&(rc.getStoredAlignment())-1) != 0) {
			throw new IllegalArgumentException("Invalid uncompressedAlignment: "+rc.getStoredAlignment());
		}
		resourceConfigs.putIfAbsent(
			PathMatcherUtil.createMatcher(rc.getPattern()),
			rc
		);
	}

	private InputStream openResource(String name) throws IOException
	{
		if (name.startsWith("classpath:")) {
			return Optional.ofNullable(classLoader.getResourceAsStream(name.substring(10)))
				.orElseThrow(() -> new FileNotFoundException("No such file in classpath: "+name));
		}
		else if (name.startsWith("file://")) {
			return new FileInputStream(name.substring(7));
		}
		else {
			throw new IOException("Unsupported protocol, only classpath is supported: file="+name);
		}
	}

	private <T> Stream<T> searchResourceConfig(
		Map<PathMatcher, Configuration.ResourceConfig> resourceConfigs,
		Path path,
		Configuration.FileType fileType,
		Function<Configuration.ResourceConfig, T> valueMapper
	)
	{
		return resourceConfigs.entrySet().stream()
			.filter(e -> valueMapper.apply(e.getValue()) != null)
			.filter(e -> e.getKey().matches(path))
			.filter(e -> Optional.ofNullable(e.getValue().getType())
				.map(ft -> ft == fileType).orElse(true)
			)
			.map(e -> valueMapper.apply(e.getValue()));
	}

	private Configuration.FileType getZipFileType(ZipArchiveEntry entry)
	{
		if (entry.isDirectory()) {
			return Configuration.FileType.dir;
		}
		else if (entry.isUnixSymlink()) {
			return Configuration.FileType.symlink;
		}
		else {
			return Configuration.FileType.file;
		}
	}

	private static class FileEntry
	{
		private ZipArchiveEntry zipEntry;

		private boolean doStore = false;

		private int alignment = 0;
	}

	@Setter
	private ClassLoader classLoader;

	@Setter
	protected Configuration configuration;

	private Map<PathMatcher, Configuration.ResourceConfig> resourceConfigs;

	private Configuration.ResourceConfig defaultResourceConfig;

	private static final ObjectMapper objectMapper = new ObjectMapper()
		.enable(JsonParser.Feature.ALLOW_COMMENTS)
		.enable(JsonParser.Feature.ALLOW_TRAILING_COMMA);

	private static final Configuration.ResourceConfig DEFAULT_RESOURCE_CONFIG = new Configuration.ResourceConfig();

	static {
		DEFAULT_RESOURCE_CONFIG.setRemove(false);
		DEFAULT_RESOURCE_CONFIG.setKeepAlignment(true);
		DEFAULT_RESOURCE_CONFIG.setMinimalCompress(3);
		DEFAULT_RESOURCE_CONFIG.setCompressedAlignment(0);
		DEFAULT_RESOURCE_CONFIG.setStoredAlignment(0);
	}
}
