package net.dryuf.maven.plugin.executablejar;

import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Plugin configuration.
 */
@Data
public class Configuration
{
	/** Script header. */
	protected String		header = "#!/usr/bin/env -S java";

	/** VM parameters. */
	protected String		vmParams;

	protected ResourceConfig	defaultResourceConfig;

	protected List<ResourceConfig>	resourceConfigs = new ArrayList<>();

	protected List<String>		externalResourceConfigs;

	/** Input jar file. */
	protected File			inputFile;

	/** Output jar file. */
	protected File			outputFile;

	@Data
	public static class ResourceConfig
	{
		private String			pattern;

		private FileType		type;

		private Boolean			remove;

		/** Minimal compression ratio to keep as compressed (0-100, 0 means no compression, 100 complete compression). */
		private Integer			minimalCompress;

		/** Keep existing alignment if present. */
		private Boolean			keepAlignment;

		/** Compressed resource alignment - 1 means unaligned, 0 default alignment. */
		private Integer			compressedAlignment;

		/** Uncompressed resource alignment - 1 means unaligned, 0 default alignment. */
		private Integer			storedAlignment;
	}

	@Data
	public static class ExternalResourceConfig
	{
		ResourceConfig defaultResourceConfig;

		List<ResourceConfig> resourceConfigs;
	}

	enum FileType
	{
		file,
		dir,
		symlink,
	}
}
