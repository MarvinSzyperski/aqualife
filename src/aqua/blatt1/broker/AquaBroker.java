package aqua.blatt1.broker;

import aqua.blatt1.client.AquaClient;
import aqua.blatt1.common.FishModel;
import messaging.Message;

import java.rmi.*;

public interface AquaBroker extends Remote {

	void respondResolutionRequest(AquaClient client, FishModel fish) throws RemoteException;

	void register(AquaClient newClient) throws RemoteException;

	void deregister(AquaClient client) throws RemoteException;

}
