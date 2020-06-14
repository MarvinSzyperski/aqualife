package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NameResolutionResponse implements Serializable {

	private InetSocketAddress address;
	private String requestID;

	public NameResolutionResponse(InetSocketAddress address, String requestID) {
		this.address = address;
		this.requestID = requestID;
	}

	public InetSocketAddress getAddress() {
		return address;
	}

	public String getRequestID() {
		return requestID;
	}
}
