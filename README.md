# dryuf-executable-jar-maven-plugin

Maven plugin aligning and uncompressing (storing) resources and optionally prepending `#!/usr/bin/env java` header into jar file.

## Usage

Typical usage is as follows:

`pom.xml` example:
```
	<plugin>
		<groupId>net.dryuf.maven.plugin</groupId>
		<artifactId>dryuf-executable-jar-maven-plugin</artifactId>
		<version>1.1.1</version>
		<executions>
			<execution>
				<phase>package</phase>
				<goals>
					<goal>create-executable</goal>
				</goals>
				<configuration>
					<noHeader>false</noHeader>                    <!-- avoid generating header, default false -->
					<header>#!/usr/bin/env java</header>          <!-- default as in example -->
					<vmParams>-Xmx16m</vmParams>                  <!-- default empty -->
					<defaultResourceConfig>
						<minimalCompress>5</minimalCompress>         <!-- minimal compression ratio to avoid storing -->
						<keepAlignment>false</keepAlignment>         <!-- keep alignment if specified in original file -->
						<compressedAlignment>4</compressedAlignment> <!-- align to 4 all compressed content -->
						<storedAlignment>16</storedAlignment>        <!-- align to 16 all stored content -->
					</defaultResourceConfig>
					<resourceConfigs>
						<resourceConfig>
							<pattern>glob:**</pattern>
							<type>dir</type>                <!-- configuration for directories -->
							<remove>true</remove>
						</resourceConfig>
					</resourceConfigs>
					<resourceConfigs>
						<resourceConfig>
							<pattern>glob:**.png</pattern>  <!-- configuration for *.png files -->
							<minimalCompress>10</minimalCompress>
							<storedAlignment>256</storedAlignment>
						</resourceConfig>
					</resourceConfigs>
					<externalResourceConfigs>                       <!-- reusable files from classpath -->
						<externalResourceConfig>classpath:ResourceConfigs-cp.json</externalResourceConfig>
						<externalResourceConfig>file://src/main/resources/ResourceConfigs-file.json</externalResourceConfig>
					</externalResourceConfigs>
					<input>${project.build.directory}/my-project-uber.jar</input> <!-- uber jar by shade plugin -->
					<output>${project.build.directory}/my-project</output>        <!-- default input without .jar suffix -->
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

This will generate `target/my-project` executable file which will launch `java -Xmx10m -jar` with the executable jar.  The files will be aligned to either maven project file configuration or external file configuration:

```
$ ./target/my-project
Hello from my-project
```


## License

The code is released under version 2.0 of the [Apache License][].

## Stay in Touch

Feel free to contact me at kvr000@gmail.com or http://github.com/kvr000 .

[Apache License]: http://www.apache.org/licenses/LICENSE-2.0
