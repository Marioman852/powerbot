package org.powerbot.script.wrappers;

import java.awt.Point;

import org.powerbot.client.AbstractModel;
import org.powerbot.client.Client;
import org.powerbot.client.CombatStatus;
import org.powerbot.client.CombatStatusData;
import org.powerbot.client.LinkedListNode;
import org.powerbot.client.RSAnimator;
import org.powerbot.client.RSCharacter;
import org.powerbot.client.RSInteractableData;
import org.powerbot.client.RSInteractableLocation;
import org.powerbot.client.RSMessageData;
import org.powerbot.client.RSNPC;
import org.powerbot.client.RSNPCNode;
import org.powerbot.client.RSPlayer;
import org.powerbot.client.Sequence;
import org.powerbot.script.lang.Filter;
import org.powerbot.script.methods.MethodContext;

public abstract class Actor extends Interactive implements Renderable, Nameable, Locatable, Drawable {
	private int faceIndex = -1;

	public Actor(final MethodContext ctx) {
		super(ctx);
	}

	protected abstract RSCharacter getAccessor();

	@Override
	public Model getModel() {
		final RSCharacter character = getAccessor();
		if (character != null && ctx.game.toolkit.graphicsIndex == 0) {
			final AbstractModel model = character.getModel();
			if (model != null) {
				return new ActorModel(ctx, model, character);
			}
		}
		return null;
	}

	public abstract int getLevel();

	public int getOrientation() {
		final RSCharacter character = getAccessor();
		return character != null ? (630 - character.getOrientation() * 45 / 2048) % 360 : 0;
	}

	public int getHeight() {
		final RSCharacter character = getAccessor();
		return character != null ? character.getHeight() : 0;
	}

	public int getAnimation() {
		final RSCharacter character = getAccessor();
		if (character == null) {
			return -1;
		}

		final RSAnimator animator = character.getAnimation();
		final Sequence sequence;
		if (animator == null || (sequence = animator.getSequence()) == null) {
			return -1;
		}
		return sequence.getID();
	}

	public int getStance() {
		final RSCharacter character = getAccessor();
		if (character == null) {
			return -1;
		}

		final RSAnimator animator = character.getPassiveAnimation();
		final Sequence sequence;
		if (animator == null || (sequence = animator.getSequence()) == null) {
			return -1;
		}
		return sequence.getID();
	}

	public int[] getAnimationQueue() {
		final RSCharacter character = getAccessor();
		if (character == null) {
			return new int[0];
		}

		final int[] arr = character.getAnimationQueue();
		return arr != null ? arr : new int[0];
	}

	public int getSpeed() {
		final RSCharacter character = getAccessor();
		return character != null ? character.isMoving() : 0;
	}

	public boolean isInMotion() {
		return getSpeed() != 0;
	}

	public static Filter<Actor> areInMotion() {
		return new Filter<Actor>() {
			@Override
			public boolean accept(final Actor actor) {
				return actor.isInMotion();
			}
		};
	}

	public String getMessage() {
		final RSCharacter character = getAccessor();
		if (character == null) {
			return "";
		}

		final RSMessageData headMessage = character.getMessageData();
		String message = "";
		if (headMessage != null && (message = headMessage.getMessage()) == null) {
			message = "";
		}
		return message;
	}

	public Actor getInteracting() {
		final Actor nil = ctx.npcs.getNil();
		final RSCharacter character = getAccessor();
		final int index = character != null ? character.getInteracting() : -1;
		if (index == -1) {
			return nil;
		}
		final Client client = ctx.getClient();
		if (client == null) {
			return nil;
		}
		if (index < 32768) {
			final Object npcNode = ctx.game.lookup(client.getRSNPCNC(), index);
			if (npcNode == null) {
				return nil;
			}
			if (npcNode instanceof RSNPCNode) {
				return new Npc(ctx, ((RSNPCNode) npcNode).getRSNPC());
			} else if (npcNode instanceof RSNPC) {
				return new Npc(ctx, (RSNPC) npcNode);
			}
			return nil;
		} else {
			final int pos = index - 32768;
			final RSPlayer[] players = client.getRSPlayerArray();
			return pos >= 0 && pos < players.length ? new Player(ctx, players[pos]) : nil;
		}
	}

	public int getAdrenalineRatio() {
		if (!isValid()) {
			return -1;
		}
		final CombatStatusData[] data = getBarData();
		if (data == null || data[0] == null) {
			return 0;
		}
		return data[0].getHPRatio();
	}

	public int getHealthRatio() {
		if (!isValid()) {
			return -1;
		}
		final CombatStatusData[] data = getBarData();
		if (data == null || data[1] == null) {
			return 100;
		}
		return data[1].getHPRatio();
	}

	public int getAdrenalinePercent() {
		if (!isValid()) {
			return -1;
		}
		final CombatStatusData[] data = getBarData();
		if (data == null || data[0] == null) {
			return 0;
		}
		return toPercent(data[0].getHPRatio());
	}

	public int getHealthPercent() {
		if (!isValid()) {
			return -1;
		}
		final CombatStatusData[] data = getBarData();
		if (data == null || data[1] == null) {
			return 100;
		}
		return toPercent(data[1].getHPRatio());
	}

	public boolean isInCombat() {
		final Client client = ctx.getClient();
		if (client == null) {
			return false;
		}
		final CombatStatusData[] data = getBarData();
		return data != null && data[1] != null && data[1].getLoopCycleStatus() < client.getLoopCycle();
	}

	public boolean isIdle() {
		return getAnimation() == -1 && !isInCombat() && !isInMotion() && !getInteracting().isValid();
	}

	public static Filter<Actor> areInCombat() {
		return new Filter<Actor>() {
			@Override
			public boolean accept(final Actor actor) {
				return actor.isInCombat();
			}
		};
	}

	@Override
	public Tile getLocation() {
		final RSCharacter character = getAccessor();
		final RelativeLocation position = getRelative();
		if (character != null && position != RelativeLocation.NIL) {
			return ctx.game.getMapBase().derive((int) position.getX() >> 9, (int) position.getY() >> 9, character.getPlane());
		}
		return Tile.NIL;
	}

	public RelativeLocation getRelative() {
		final RSCharacter character = getAccessor();
		final RSInteractableData data = character != null ? character.getData() : null;
		final RSInteractableLocation location = data != null ? data.getLocation() : null;
		if (location != null) {
			return new RelativeLocation(location.getX(), location.getY());
		}
		return RelativeLocation.NIL;
	}

	@Override
	public Point getInteractPoint() {
		final RSCharacter character = getAccessor();
		if (character == null) {
			return new Point(-1, -1);
		}

		final Model model = getModel();
		if (model != null) {
			Point point = model.getCentroid(faceIndex);
			if (point != null) {
				return point;
			}
			point = model.getCentroid(faceIndex = model.nextTriangle());
			if (point != null) {
				return point;
			}
		}
		final RenderableCuboid cuboid = new RenderableCuboid(ctx, character);
		return cuboid.getInteractPoint();
	}

	@Override
	public Point getNextPoint() {
		final RSCharacter character = getAccessor();
		if (character == null) {
			return new Point(-1, -1);
		}

		final Model model = getModel();
		if (model != null) {
			return model.getNextPoint();
		}
		final RenderableCuboid cuboid = new RenderableCuboid(ctx, character);
		return cuboid.getNextPoint();
	}

	@Override
	public Point getCenterPoint() {
		final RSCharacter character = getAccessor();
		if (character == null) {
			return new Point(-1, -1);
		}

		final Model model = getModel();
		if (model != null) {
			return model.getCenterPoint();
		}
		final RenderableCuboid cuboid = new RenderableCuboid(ctx, character);
		return cuboid.getCenterPoint();
	}

	@Override
	public boolean contains(final Point point) {
		final RSCharacter character = getAccessor();
		if (character == null) {
			return false;
		}

		final Model model = getModel();
		if (model != null) {
			return model.contains(point);
		}
		final RenderableCuboid cuboid = new RenderableCuboid(ctx, character);
		return cuboid.contains(point);
	}

	private Point getScreenPoint() {
		final RSCharacter character = getAccessor();
		final RSInteractableData data = character != null ? character.getData() : null;
		final RSInteractableLocation location = data != null ? data.getLocation() : null;
		if (location != null) {
			return ctx.game.groundToScreen((int) location.getX(), (int) location.getY(), character.getPlane(), character.getHeight() / 2);
		}
		return new Point(-1, -1);
	}

	private LinkedListNode[] getBarNodes() {
		final RSCharacter accessor = getAccessor();
		if (accessor == null) {
			return null;
		}
		final org.powerbot.client.LinkedList barList = accessor.getCombatStatusList();
		if (barList == null) {
			return null;
		}
		final LinkedListNode tail = barList.getTail();
		final LinkedListNode health;
		final LinkedListNode adrenaline;
		final LinkedListNode current;
		current = tail.getNext();
		if (current.getNext() != tail) {
			adrenaline = current;
			health = current.getNext();
		} else {
			adrenaline = null;
			health = current;
		}

		return new LinkedListNode[]{adrenaline, health};
	}

	private CombatStatusData[] getBarData() {
		final LinkedListNode[] nodes = getBarNodes();
		if (nodes == null) {
			return null;
		}
		final CombatStatusData[] data = new CombatStatusData[nodes.length];
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i] == null || !(nodes[i] instanceof CombatStatus)) {
				data[i] = null;
				continue;
			}
			final CombatStatus status = (CombatStatus) nodes[i];
			final org.powerbot.client.LinkedList statuses = status.getData();
			if (statuses == null) {
				data[i] = null;
				continue;
			}

			final LinkedListNode node = statuses.getTail().getNext();
			if (node == null || !(node instanceof CombatStatusData)) {
				data[i] = null;
				continue;
			}
			data[i] = (CombatStatusData) node;
		}
		return data;
	}

	private int toPercent(final int ratio) {
		return (int) Math.ceil((ratio * 100d) / 255);
	}

	@Override
	public int hashCode() {
		final RSCharacter i;
		return (i = this.getAccessor()) != null ? System.identityHashCode(i) : 0;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || !(o instanceof Actor)) {
			return false;
		}
		final Actor c = (Actor) o;
		final RSCharacter i;
		return (i = this.getAccessor()) != null && i == c.getAccessor();
	}
}
