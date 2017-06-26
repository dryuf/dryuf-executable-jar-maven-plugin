package net.dryuf.maven.plugin.executablejar.concurrent;

import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;


/**
 * Tests for {@link ResultSerializingExecutor}.
 */
public class ResultSerializingExecutorTest
{
	@Test
	public void testShort() throws InterruptedException
	{
		for (int t = 0; t < 1024; ++t) {
			try (ResultSerializingExecutor rse = new ResultSerializingExecutor()) {
				for (int i = ThreadLocalRandom.current().nextInt(512); --i >= 0; ) {
					rse.submit(this::doLittle);
				}
			}
		}
	}

	private Void doLittle()
	{
		littleVar *= 17;
		return null;
	}

	private int littleVar = 13;
}
