package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.msgtypes.*;
import aqua.blatt2.broker.PoisonPill;
import messaging.Endpoint;
import messaging.Message;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

	public class Broker {

	Endpoint endpoint = new Endpoint(4711);
	ClientCollection<InetSocketAddress> collection = new ClientCollection();
	ExecutorService executorService = Executors.newFixedThreadPool(20);
	ReadWriteLock lock = new ReentrantReadWriteLock();
	volatile static Boolean stopRequested = false;
	public static int counter = 0;

	public static void main(String[] args) {
		Broker broker = new Broker();
		broker.broker();
	}

	public void broker(){

		Thread end = new Thread(()-> {
			JOptionPane.showMessageDialog(null, "Press Ok to end the server");
			Broker.stopRequested = true;
		});
		//end.start();


		while(!stopRequested){

			Message message = endpoint.blockingReceive();
			if(message.getPayload() instanceof PoisonPill) break;
			executorService.execute(()-> new BrokerTask(message));

		}
		executorService.shutdown();
	}



	public class BrokerTask {

		public BrokerTask(Message message){

				 if(message.getPayload() instanceof RegisterRequest){
					register(message);
				}
				if(message.getPayload() instanceof DeregisterRequest){
					deregister(message);
				}
				if(message.getPayload() instanceof HandoffRequest){
					handoffish(message);
				}

		}

		public void register(Message message){
			InetSocketAddress newClient = message.getSender();
			lock.writeLock().lock();
			if(counter==0) endpoint.send(newClient,new Token());
			counter++;

			collection.add("tank"+collection.size(), newClient);
			InetSocketAddress leftClient = collection.getLeftNeighborOf(newClient);
			InetSocketAddress rightClient = collection.getRightNeighborOf(newClient);
			endpoint.send(newClient, new RegisterResponse("tank"+collection.size()));
			endpoint.send(newClient,new NeighborUpdate(
					leftClient,rightClient
			));

			endpoint.send(leftClient,new NeighborUpdate(collection.getLeftNeighborOf(leftClient),newClient));
			endpoint.send(rightClient,new NeighborUpdate(newClient,collection.getRightNeighborOf(newClient)));
			lock.writeLock().unlock();
		}

		public void deregister(Message message){
			lock.writeLock().lock();
			InetSocketAddress client = message.getSender();
			InetSocketAddress leftClient = collection.getLeftNeighborOf(client);
			InetSocketAddress rightClient = collection.getRightNeighborOf(client);
			endpoint.send(leftClient,new NeighborUpdate(collection.getLeftNeighborOf(leftClient),rightClient));
			endpoint.send(rightClient,new NeighborUpdate(leftClient,collection.getRightNeighborOf(rightClient)));
			collection.remove(collection.indexOf(client));
			counter--;
			lock.writeLock().unlock();
		}

		public void handoffish(Message message){
			lock.readLock().lock();
			HandoffRequest temp = (HandoffRequest) message.getPayload();
			InetSocketAddress sender = message.getSender();
			if(temp.getFish().getDirection().equals(Direction.LEFT)){
				endpoint.send(collection.getLeftNeighborOf(sender),message.getPayload());
			}else{
				endpoint.send(collection.getRightNeighborOf(sender),message.getPayload());
			}

			lock.readLock().unlock();
		}



	}

}
