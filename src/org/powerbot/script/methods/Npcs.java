package org.powerbot.script.methods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.powerbot.client.Client;
import org.powerbot.client.HashTable;
import org.powerbot.client.RSNPC;
import org.powerbot.client.RSNPCNode;
import org.powerbot.script.lang.GameDualQuery;
import org.powerbot.script.wrappers.Npc;

/**
 * {@link Npcs} is a static utility which provides access to {@link Npc}s in the game.
 * <p/>
 * {@link Npc}s are only accessible within the mini-map's range.
 *
 * @author Timer
 */
public class Npcs extends GameDualQuery<Npc> {
	public Npcs(ClientFactory factory) {
		super(factory);
	}

	/**
	 * Returns all the {@link Npc}s in the region.
	 *
	 * @return an array of the loaded {@link Npc}s
	 */
	@Override
	protected List<Npc> get() {
		final List<Npc> items = new ArrayList<>();

		Client client = ctx.getClient();
		if (client == null) {
			return items;
		}

		final int[] indices = client.getRSNPCIndexArray();
		final HashTable npcTable = client.getRSNPCNC();
		if (indices == null || npcTable == null) {
			return items;
		}

		for (final int index : indices) {
			Object npc = ctx.game.lookup(npcTable, index);
			if (npc == null) {
				continue;
			}
			if (npc instanceof RSNPCNode) {
				npc = ((RSNPCNode) npc).getRSNPC();
			}
			if (npc instanceof RSNPC) {
				items.add(new Npc(ctx, (RSNPC) npc));
			}
		}

		return items;
	}
}
