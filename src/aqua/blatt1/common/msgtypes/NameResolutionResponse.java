package aqua.blatt1.common.msgtypes;

import aqua.blatt1.client.AquaClient;

import java.io.Serializable;

public class NameResolutionResponse implements Serializable {

	private AquaClient address;
	private String requestID;

	public NameResolutionResponse(AquaClient address, String requestID) {
		this.address = address;
		this.requestID = requestID;
	}

	public AquaClient getAddress() {
		return address;
	}

	public String getRequestID() {
		return requestID;
	}
}
