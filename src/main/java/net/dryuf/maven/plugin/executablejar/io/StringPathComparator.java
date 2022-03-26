package net.dryuf.maven.plugin.executablejar.io;

import java.util.Comparator;

public class StringPathComparator implements Comparator<String>
{
	public static final StringPathComparator INSTANCE = new StringPathComparator();

	@Override
	public int compare(String f, String s)
	{
		for (int i = 0, stop = Math.min(f.length(), s.length()); i < stop; ++i) {
			char cf = f.charAt(i), cs = s.charAt(i);
			if (cf == cs)
				continue;
			if (cf == '/')
				return -1;
			else if (cs == '/')
				return 1;
			return cf-cs;
		}
		return Math.subtractExact(f.length(), s.length());
	}
}
