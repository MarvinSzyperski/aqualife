package aqua.blatt1.broker;

import aqua.blatt1.Security.SecureAsymmetricEndpoint;
import aqua.blatt1.client.AquaClient;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.NameResolutionResponse;
import aqua.blatt1.common.msgtypes.Token;
import messaging.Endpoint;

import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker implements AquaBroker, Serializable {

	ClientCollection<AquaClient> collection = new ClientCollection();
	ReadWriteLock lock = new ReentrantReadWriteLock();
	public static int counter = 0;

	public static void main(String[] args) throws RemoteException, AlreadyBoundException {
		Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
		AquaBroker stub = (AquaBroker) UnicastRemoteObject.exportObject(new Broker(), 0);
		registry.bind(Properties.BROKER_NAME, stub);

	}


	public void register(AquaClient newClient) throws RemoteException {
		lock.writeLock().lock();
		if (counter == 0) newClient.receiveToken(new Token());
		counter++;

		collection.add("tank" + (collection.size() + 1), newClient);
		AquaClient leftClient = collection.getLeftNeighborOf(newClient);
		AquaClient rightClient = collection.getRightNeighborOf(newClient);
		newClient.onRegistration("tank" + collection.size());
		newClient.receiveNeighbor(leftClient, rightClient);

		leftClient.receiveNeighbor(collection.getLeftNeighborOf(leftClient), newClient);
		rightClient.receiveNeighbor(newClient, collection.getRightNeighborOf(rightClient));
		lock.writeLock().unlock();
	}

	public void deregister(AquaClient client) throws RemoteException {
		lock.writeLock().lock();

		AquaClient leftClient = collection.getLeftNeighborOf(client);
		AquaClient rightClient = collection.getRightNeighborOf(client);
		leftClient.receiveNeighbor(collection.getLeftNeighborOf(leftClient),rightClient);
		rightClient.receiveNeighbor(leftClient,collection.getRightNeighborOf(rightClient));
		collection.remove(collection.indexOf(client));
		counter--;
		lock.writeLock().unlock();
	}

/*	public void handoffish(AquaClient client) {
		lock.readLock().lock();
		HandoffRequest temp = (HandoffRequest) message.getPayload();
		InetSocketAddress sender = message.getSender();
		if (temp.getFish().getDirection().equals(Direction.LEFT)) {
			endpoint.send(collection.getLeftNeighborOf(sender), message.getPayload());
		} else {
			endpoint.send(collection.getRightNeighborOf(sender), message.getPayload());
		}

		lock.readLock().unlock();
	}
*/

	public void respondResolutionRequest(AquaClient sender,FishModel fish) throws RemoteException {
		int index = collection.indexOf(fish.getTankId());

		AquaClient client = collection.getClient(index);
		sender.receiveResolutionResponse(new NameResolutionResponse(client, fish.getId()));

	}


}


