package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.RecordMode;
import aqua.blatt1.common.msgtypes.NeighborUpdate;
import aqua.blatt1.common.msgtypes.SnapshotMarker;
import aqua.blatt1.common.msgtypes.SnapshotToken;
import aqua.blatt1.common.msgtypes.Token;

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
	private volatile int state;
	private RecordMode mode;

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
			state++;
		}
	}

	synchronized void receiveFish(FishModel fish) {
		fish.setToStart();
		fishies.add(fish);
		if(mode==RecordMode.IDLE) return;
		if(fish.getDirection()==Direction.LEFT){
			if(mode==RecordMode.BOTH||mode==RecordMode.RIGHT) state++;
		}
		if(fish.getDirection()==Direction.RIGHT){
			if(mode==RecordMode.BOTH||mode==RecordMode.LEFT) state++;
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

	public void initiateSnapshot(){
		state = fishies.size();
		mode = RecordMode.BOTH;
		forwarder.sendMarker(leftNeighbor);
		forwarder.sendMarker(rightNeighbor);
		while (mode != RecordMode.IDLE);
		forwarder.sendSToken(leftNeighbor,new SnapshotToken(this.id,this.state));
	}

	public void receiveMarker(InetSocketAddress sender) {
		if(mode==RecordMode.IDLE){
			state = fishies.size();
			if (sender==leftNeighbor) mode=RecordMode.RIGHT;
			else if (sender == rightNeighbor) mode = RecordMode.LEFT;
			else state=-500; //Look for errors
			forwarder.sendMarker(leftNeighbor);
			forwarder.sendMarker(rightNeighbor);
			}

		else {
			try {
				mode=(sender.equals(leftNeighbor))?removeLeft():removeRight();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}


	public void receiveSToken(SnapshotToken stk){
		while (mode != RecordMode.IDLE);

		if(!stk.id.equals(this.id)) {
			stk.value+=state;
			forwarder.sendSToken(leftNeighbor,stk);
		}
		JOptionPane.showMessageDialog(null,stk.value);
	}

	public RecordMode removeLeft() throws Exception {
		if(this.mode==RecordMode.BOTH) return RecordMode.RIGHT;
		if(this.mode==RecordMode.LEFT) return RecordMode.IDLE;
		if(leftNeighbor.equals(rightNeighbor)) return RecordMode.IDLE;
		throw  new Exception("MISSING DIRECTION");
	}

	public RecordMode removeRight() throws Exception {
		if(this.mode==RecordMode.BOTH) return RecordMode.LEFT;
		if(this.mode==RecordMode.RIGHT) return RecordMode.IDLE;
		throw new Exception("MISSING DIRECTION");
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
						state--;
					} else {
						forwarder.handOff(fish, rightNeighbor);
						state--;
					}
				}

				if (fish.disappears())
					it.remove();
					state--;
			}
		}
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