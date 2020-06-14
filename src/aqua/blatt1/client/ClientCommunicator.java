package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;

public class ClientCommunicator {
	private final Endpoint endpoint;
	private Timer timer;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish, java.net.InetSocketAddress receiver) {

			endpoint.send(receiver, new HandoffRequest(fish));
		}

		public void sendToken(InetSocketAddress receiver, Token tk){
			endpoint.send(receiver,tk);
		}

		public void findFish(String fishID, InetSocketAddress receiver) {

			endpoint.send(receiver,new LocationRequest(fishID));
		}

		public void nameRequest(String tankId,String fishId){
			endpoint.send(broker,new NameResolutionRequest(tankId,fishId));
		}

		public void locationUpdate(InetSocketAddress receiver,String fishID){
			endpoint.send(receiver,new LocationUpdate(fishID));
		}
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse) {
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());
					timer = new Timer();
					timer.schedule(new TimerTask() {
						@Override
						public void run() {
							//Reregistering
						}
					},0);
				}


				else if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

				else if(msg.getPayload() instanceof NeighborUpdate)
					tankModel.receiveNeighbor(((NeighborUpdate) msg.getPayload()).getLeft(), ((NeighborUpdate) msg.getPayload()).getRight());
				//	tankModel.receiveNeighbor((NeighborUpdate) msg.getPayload());

				else if(msg.getPayload() instanceof Token)
					tankModel.receiveToken((Token) msg.getPayload());

				else if(msg.getPayload() instanceof LocationRequest)
					tankModel.receiveLocationRequest((LocationRequest)msg.getPayload());

				else if(msg.getPayload() instanceof NameResolutionResponse)
					tankModel.receiveResolutionResponse((NameResolutionResponse) msg.getPayload());

				else if(msg.getPayload() instanceof LocationUpdate)
					tankModel.locationUpdate((LocationUpdate)msg.getPayload(),msg.getSender());


			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
