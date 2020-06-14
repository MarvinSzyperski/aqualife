package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;
	private final int lifetime;

	public RegisterResponse(String id, int lifetime) {
		this.id = id;
		this.lifetime = lifetime;
	}

	public String getId() {
		return id;
	}

	public int getLifetime() {
		return lifetime;
	}
}
