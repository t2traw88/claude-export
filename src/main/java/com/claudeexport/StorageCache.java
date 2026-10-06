package com.claudeexport;

import java.util.ArrayList;
import java.util.List;

/**
 * Last-seen seed vault and potion storage, saved to storage-cache.json so they survive
 * restarts. Like BankCache, it is never changed after creation; a new copy replaces it.
 */
class StorageCache
{
	static final String FILE = "storage-cache.json";

	long accountHash;
	String seedVaultSeen;
	List<BankCache.Entry> seedVault = new ArrayList<>();
	String potionsSeen;
	// qty here is the number of doses stored
	List<BankCache.Entry> potions = new ArrayList<>();

	/** A copy for the same account, so one section can be replaced while keeping the other. */
	StorageCache copyFor(long hash)
	{
		StorageCache c = new StorageCache();
		c.accountHash = hash;
		if (accountHash == hash)
		{
			c.seedVaultSeen = seedVaultSeen;
			c.seedVault = seedVault;
			c.potionsSeen = potionsSeen;
			c.potions = potions;
		}
		return c;
	}
}
