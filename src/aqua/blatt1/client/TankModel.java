package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Position;
import aqua.blatt1.common.msgtypes.*;

import javax.swing.*;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;
	protected InetSocketAddress leftNeighbor;
	protected InetSocketAddress rightNeighbor;
	private boolean token;
	protected Timer timer;
	private Map<String, InetSocketAddress> fishMap = new HashMap<>();


	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id,int duration) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				forwarder.register();
			}
		},duration-10);
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = Math.min(x, WIDTH - FishModel.getXSize() - 1);
			y = Math.min(y, HEIGHT - FishModel.getYSize());

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
			fishMap.put(fish.getId(),null);


		}
	}

	synchronized void receiveFish(FishModel fish) {
		fish.setToStart();
		fishies.add(fish);
		if(fishMap.containsKey(fish.getId())){
			fishMap.put(fish.getId(),null);
		}else{
			forwarder.nameRequest(fish.getTankId(),fish.getId());
		}

	}

	synchronized void receiveNeighbor(NeighborUpdate n) {
		this.leftNeighbor = n.getLeft();
		this.rightNeighbor = n.getRight();
	}

	synchronized void receiveNeighbor(InetSocketAddress l, InetSocketAddress r){

		this.leftNeighbor = l;
		this.rightNeighbor = r;
	}

	synchronized void receiveToken(Token tk){
		this.token = true;
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				token = false;
				forwarder.sendToken(leftNeighbor, tk);
			}
		},2000);
	}


	public String getId() {
		return id;
	}

	public boolean hasToken() { return token;}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (leftNeighbor == null && rightNeighbor == null) {
				receiveFish(fish);
			} else {
				if (fish.hitsEdge()) {
					if (!token) fish.reverse();
					else if (fish.getDirection().equals(Direction.LEFT)) {
						forwarder.handOff(fish, leftNeighbor);

					} else {
						forwarder.handOff(fish, rightNeighbor);
					}
				}

				if (fish.disappears())
					it.remove();
					fishMap.remove(fish);
			}
		}
	}


	public void locateFishGlobally(String fishID) {
		System.out.println("Looking for fish "+fishID);
		if(fishMap.get(fishID) == null){
			locateFishLocally(fishID);
		}else{
			forwarder.findFish(fishID,fishMap.get(fishID));
		}

	}

	public void locateFishLocally(String fishId) {

		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();
			if(fish.getId().equals(fishId)) fish.toggle();
		}

	}

	public void receiveLocationRequest(LocationRequest payload) {

	locateFishGlobally(payload.getFishId());

	}

	public void receiveResolutionResponse(NameResolutionResponse payload){
		forwarder.locationUpdate(payload.getAddress(),payload.getRequestID());
	}

	public void locationUpdate(LocationUpdate payload, InetSocketAddress newLoc) {

		fishMap.put(payload.getFishID(),newLoc);

	}


	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() {
		forwarder.deregister(id);
	}

}