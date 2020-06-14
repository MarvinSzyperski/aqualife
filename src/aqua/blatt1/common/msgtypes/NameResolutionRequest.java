package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class NameResolutionRequest implements Serializable {

	private String tankID;
	private String requestID;

	public NameResolutionRequest(String tankID, String requestID) {
		this.tankID = tankID;
		this.requestID = requestID;
	}

	public String getTankID() {
		return tankID;
	}

	public String getRequestID() {
		return requestID;
	}
}
