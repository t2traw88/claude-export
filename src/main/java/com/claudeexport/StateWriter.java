package com.claudeexport;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import net.runelite.client.util.Filepath;

/**
 * Writes files so a reader never sees a half-written file: the content goes to a
 * temp file first, which is then renamed over the real file in one step.
 */
final class StateWriter
{
	private StateWriter()
	{
	}

	static void writeAtomically(Filepath dir, String fileName, String content) throws IOException
	{
		dir.createDirectories();
		Filepath target = dir.joinSegment(fileName);
		Filepath tmp = dir.createTempFile(fileName + "-", ".tmp");
		try
		{
			tmp.write(content);
			try
			{
				tmp.moveTo(target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (AtomicMoveNotSupportedException e)
			{
				// Rare (e.g. some network drives); a plain replace is the best we can do
				tmp.moveTo(target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally
		{
			// Only still exists if something above failed; don't leave temp files behind
			tmp.deleteIfExists();
		}
	}
}
