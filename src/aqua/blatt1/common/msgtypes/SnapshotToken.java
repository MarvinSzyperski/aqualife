package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public final class SnapshotToken implements Serializable {

	public SnapshotToken(String id, int v) {
		this.id = id;
		this.value = v;
	}

	public String id;
	public int value = 0;

}
