package aqua.blatt1.client;

import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.NameResolutionResponse;
import aqua.blatt1.common.msgtypes.Token;

import java.net.InetSocketAddress;
import java.rmi.*;

public interface AquaClient extends Remote {

	void receiveFish(FishModel fishModel) throws RemoteException;

	void locateFishGlobally(String fishID) throws RemoteException;

	//void nameRequest(String tankId, String fishId);

	void locationUpdate(String fishID, AquaClient newLoc) throws RemoteException;

	void receiveToken(Token tk) throws RemoteException;

	void onRegistration(String id) throws RemoteException;

	void receiveResolutionResponse(NameResolutionResponse payload) throws RemoteException;

	void receiveNeighbor(AquaClient l, AquaClient r) throws RemoteException;

}
