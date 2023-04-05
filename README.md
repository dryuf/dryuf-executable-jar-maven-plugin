# dryuf-executable-jar-maven-plugin

Maven plugin aligning and uncompressing (storing) resources and optionally prepending `#!/usr/bin/env java` header into jar file.


## Usage

Typical usage is as follows:

`pom.xml` example:
```
	<plugin>
		<groupId>net.dryuf.maven.plugin</groupId>
		<artifactId>dryuf-executable-jar-maven-plugin</artifactId>
		<version>1.2.3</version>
		<executions>
			<execution>
				<phase>package</phase>
				<goals>
					<goal>create-executable</goal>
				</goals>
				<configuration>
					<!-- avoid generating header, default false -->
					<noHeader>false</noHeader>
					<!-- executable header, default as in example -->
					<header>#!/usr/bin/env -S java</header>
					<!-- additional arguments, default empty -->
					<vmParams>-Xmx16m</vmParams>
					<!-- sort files by directory structure and file names in output file -->
					<sort>true</sort>
					<!-- default configuration -->
					<!-- each of the fields is optional in any level of configuration, if missing the next matching is chosen -->
					<defaultResourceConfig>
						<!-- minimal compression ratio to avoid storing -->
						<minimalCompress>5</minimalCompress>
						<!-- keep alignment if specified in original file -->
						<keepAlignment>false</keepAlignment>
						<!-- align to 4 all compressed content -->
						<compressedAlignment>4</compressedAlignment>
						<!-- align to 16 all stored content -->
						<storedAlignment>16</storedAlignment>
					</defaultResourceConfig>
					<resourceConfigs>
						<resourceConfig>
							<pattern>glob:**</pattern>
							<!-- configuration for directories -->
							<type>dir</type>
							<remove>true</remove>
						</resourceConfig>
					</resourceConfigs>
					<resourceConfigs>
						<resourceConfig>
							<!-- configuration for *.png files -->
							<pattern>glob:**.png</pattern>
							<minimalCompress>10</minimalCompress>
							<storedAlignment>256</storedAlignment>
						</resourceConfig>
					</resourceConfigs>
					<!-- reusable configuration files from classpath -->
					<externalResourceConfigs>
						<externalResourceConfig>classpath:ResourceConfigs-cp.json</externalResourceConfig>
						<externalResourceConfig>file://src/main/resources/ResourceConfigs-file.json</externalResourceConfig>
					</externalResourceConfigs>
					<!-- uber jar by shade plugin -->
					<input>${project.build.directory}/my-project-uber.jar</input>
					<!-- default input without .jar suffix -->
					<output>${project.build.directory}/my-project</output>
				</configuration>
			</execution>
		</executions>
	</plugin>
```

`ResourceConfigs-cp.json` example:
```
{
	"defaultResourceConfig": {
		"minimalCompress": 5,
		"storedAlignment": 16,
		"compressedAlignment": 4,
	},
	"resourceConfigs": [
		{
			"pattern": "glob:**.align64",
			"minimalCompress": 101,
			"storedAlignment": 64,
		}
	]
}
```

This will generate `target/my-project` executable file which will launch `java -Xmx16m -jar` with the executable jar.  The files will be aligned to either maven project file configuration or external file configuration:

```
$ ./target/my-project
Hello from my-project
```


## License

The code is released under version 2.0 of the [Apache License][].

## Stay in Touch

Feel free to contact me at kvr000@gmail.com or http://github.com/kvr000 .

[Apache License]: http://www.apache.org/licenses/LICENSE-2.0
