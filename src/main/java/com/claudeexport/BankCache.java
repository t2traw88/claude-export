package com.claudeexport;

import java.util.ArrayList;
import java.util.List;

/**
 * The last bank contents we saw. The bank can only be read while it's open, so this is
 * kept in memory and saved to bank-cache.json so it survives client restarts.
 * Never modified after it's created; a new one replaces it each time the bank changes.
 */
class BankCache
{
	static final String FILE = "bank-cache.json";

	// Which account this bank belongs to, so an alt never exports your main's bank
	long accountHash;
	String lastSeen;
	List<Entry> items = new ArrayList<>();

	static class Entry
	{
		int id;
		String name;
		int qty;
	}
}
