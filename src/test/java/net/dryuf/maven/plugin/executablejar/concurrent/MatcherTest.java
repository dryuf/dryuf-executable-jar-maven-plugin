package net.dryuf.maven.plugin.executablejar.concurrent;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;


/**
 * Tests PathMatcher.
 */
public class MatcherTest
{
	@Test
	public void testRecursiveExpandNoslash()
	{
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.java");
		Assert.assertTrue(matcher.matches(Paths.get("My.java")));
		Assert.assertTrue(matcher.matches(Paths.get("a/My.java")));
		Assert.assertTrue(matcher.matches(Paths.get("a/b/My.java")));
	}

	@Test
	public void testRecursiveExpandSlash()
	{
		// Double star slash behaves differently in Java core and Ant, Ant would still match root file
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");
		Assert.assertFalse(matcher.matches(Paths.get("My.java")));
		Assert.assertTrue(matcher.matches(Paths.get("a/My.java")));
		Assert.assertTrue(matcher.matches(Paths.get("a/b/My.java")));
	}
}
