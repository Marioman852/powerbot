package org.powerbot.bot.rt4.client;

import org.powerbot.bot.ReflectProxy;
import org.powerbot.bot.Reflector;

public class EntryList extends ReflectProxy {
	public EntryList(final Reflector reflector, final Object obj) {
		super(reflector, obj);
	}

	public Entry getSentinel() {
		return new Entry(reflector, reflector.access(this));
	}
}
