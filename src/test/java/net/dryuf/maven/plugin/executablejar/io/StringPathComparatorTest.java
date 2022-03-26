package net.dryuf.maven.plugin.executablejar.io;

import com.google.common.collect.ImmutableList;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class StringPathComparatorTest
{
	private final StringPathComparator instance = new StringPathComparator();

	@Test
	public void testSame()
	{
		int cmp = instance.compare("same", "same");
		assertThat(cmp).isZero();
	}

	@Test
	public void testLower()
	{
		int cmp = instance.compare("sama", "same");
		assertThat(cmp).isNegative();
	}

	@Test
	public void testGreater()
	{
		int cmp = instance.compare("same", "sama");
		assertThat(cmp).isPositive();
	}

	@Test
	public void testSubdirLowerExt()
	{
		int cmp = instance.compare("same/file", "same.txt");
		assertThat(cmp).isNegative();
	}

	@Test
	public void testSubdirLowerName()
	{
		int cmp = instance.compare("same/file", "samecont");
		assertThat(cmp).isNegative();
	}

	@Test
	public void testSubdirSecondGreaterExt()
	{
		int cmp = instance.compare("same.txt", "same/file");
		assertThat(cmp).isPositive();
	}

	@Test
	public void testSubdirSecondGreaterName()
	{
		int cmp = instance.compare("samecont", "same/file");
		assertThat(cmp).isPositive();
	}

	@Test
	public void testSort()
	{
		List<String> sorted = Stream.of("", "a", "b/", "a/sub", "a/sub.ext", "a/sub/more", "a/subcont", "a/first", "b$subclass", "b/sub")
				.sorted(instance)
				.collect(ImmutableList.toImmutableList());
		assertThat(sorted).isEqualTo(ImmutableList.of(
				"", "a", "a/first", "a/sub", "a/sub/more", "a/sub.ext", "a/subcont", "b/", "b/sub", "b$subclass"
		));
	}
}
