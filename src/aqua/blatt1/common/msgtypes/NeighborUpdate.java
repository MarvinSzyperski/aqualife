package aqua.blatt1.common.msgtypes;

import aqua.blatt1.common.Direction;

import java.io.Serializable;
import java.net.InetSocketAddress;

public final class NeighborUpdate implements Serializable {
	private final InetSocketAddress left;
	private final InetSocketAddress right;

	public NeighborUpdate(InetSocketAddress l,InetSocketAddress r) {
		this.left = l;
		this.right = r;
	}

	public InetSocketAddress getLeft() {
		return left;
	}

	public InetSocketAddress getRight() {
		return right;
	}
}
