package aqua.blatt1.client;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.broker.AquaBroker;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.*;

public class TankModel extends Observable implements Iterable<FishModel>,AquaClient{

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final AquaBroker broker;
	protected AquaClient leftNeighbor;
	protected AquaClient rightNeighbor;
	private boolean token;
	protected Timer timer;
	private Map<String, AquaClient> fishMap = new HashMap<>();
	public AquaClient client;


	public TankModel(AquaBroker broker) throws RemoteException {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.broker = broker;

		this.client = (AquaClient) UnicastRemoteObject.exportObject(this,0);

	}

	public void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	public void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = Math.min(x, WIDTH - FishModel.getXSize() - 1);
			y = Math.min(y, HEIGHT - FishModel.getYSize());

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
			fishMap.put(fish.getId(),this);


		}
	}

	public void receiveFish(FishModel fish) throws RemoteException {
		fish.setToStart();
		fishies.add(fish);
		if(fishMap.containsKey(fish.getId())){
			fishMap.put(fish.getId(),null);
		}else{
			broker.respondResolutionRequest(this,fish);
					//new Message(new NameResolutionRequest(fish.getTankId(),fish.getId()),null));
		}

	}

	public void receiveNeighbor(AquaClient l, AquaClient r) {
		this.leftNeighbor = r;
		this.rightNeighbor = l;
	}


	public void receiveToken(Token tk){
		this.token = true;
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				token = false;
				try {
					leftNeighbor.receiveToken(tk);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		},2000);
	}


	public String getId() {
		return id;
	}

	public boolean hasToken() { return token;}

	public int getFishCounter() {
		return fishCounter;
	}

	public Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private void updateFishies() throws RemoteException {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (leftNeighbor == null && rightNeighbor == null) {
				receiveFish(fish);
			} else {
				if (fish.hitsEdge()) {
					if (!token) fish.reverse();
					else if (fish.getDirection().equals(Direction.LEFT)) {
						leftNeighbor.receiveFish(fish);

					} else {
						rightNeighbor.receiveFish(fish);
					}
				}

				if (fish.disappears())
					it.remove();
					fishMap.remove(fish);
			}
		}
	}


	public void locateFishGlobally(String fishID) throws RemoteException {
		System.out.println("Looking for fish "+fishID);
		if(fishMap.get(fishID) == null){
			locateFishLocally(fishID);
		}else{
			fishMap.get(fishID).locateFishGlobally(fishID);
		}

	}

	public void locateFishLocally(String fishId) {

		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();
			if(fish.getId().equals(fishId)) fish.toggle();
		}

	}

	public void receiveLocationRequest(LocationRequest payload) throws RemoteException {

	locateFishGlobally(payload.getFishId());

	}

	public void receiveResolutionResponse(NameResolutionResponse payload) throws RemoteException {
		payload.getAddress().locationUpdate(payload.getRequestID(),payload.getAddress());
	}

	public void locationUpdate(String fishID, AquaClient newLoc) {

		fishMap.put(fishID,newLoc);

	}


	private void update() throws RemoteException {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() throws RemoteException {
		broker.register(this.client);

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public void finish() throws RemoteException {
		broker.deregister(this);
	}

}