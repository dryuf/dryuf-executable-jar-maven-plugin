# dryuf-executable-jar-maven-plugin

Maven plugin prepending `#!/usr/bin/env java` header into jar file.

## Usage

Typical usage is as follows:

`pom.xml` :
```
	<plugin>
		<groupId>net.dryuf.maven.plugin</groupId>
		<artifactId>dryuf-executable-jar-maven-plugin</artifactId>
		<version>0.0.1</version>
		<executions>
			<execution>
				<phase>package</phase>
				<goals>
					<goal>create-executable</goal>
				</goals>
				<configuration>
					<header>#!/usr/bin/env java</header>   <!-- default as in example -->
					<vmParamsarams>-Xmx10m</vmParamsarams> <!-- default empty -->
					<input>${project.build.directory}/my-project-uber.jar</input> <!-- uber jar by shade plugin -->
					<output>${project.build.directory}/my-project</output>        <!-- default input without .jar suffix -->
				</configuration>
			</execution>
		</executions>
	</plugin>
```

This will generate `target/my-project` executable file which will launch `java -Xmx10m` with the executable jar:

```
$ ./target/my-project
Hello from my-project
```


## License

The code is released under version 2.0 of the [Apache License][].

## Stay in Touch

Feel free to contact me at kvr000@gmail.com or http://github.com/kvr000 .

[Apache License]: http://www.apache.org/licenses/LICENSE-2.0
