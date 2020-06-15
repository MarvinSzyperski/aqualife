package aqua.blatt1.broker;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/*
 * This class is not thread-safe and hence must be used in a thread-safe way, e.g. thread confined or 
 * externally synchronized. 
 */

public class ClientCollection<T> {
	private class Client {
		final String id;
		final T client;
		Timestamp timestamp;

		Client(String id, T client, Timestamp timestamp) {
			this.id = id;
			this.client = client;
			this.timestamp = timestamp;
		}

		public void setTimestamp(Timestamp timestamp) {
			this.timestamp = timestamp;
		}
	}

	private final List<Client> clients;

	public ClientCollection() {
		clients = new ArrayList<Client>();
	}

	public ClientCollection<T> add(String id, T client, Timestamp timestamp) {
		clients.add(new Client(id, client,timestamp));
		return this;
	}

	public ClientCollection<T> remove(int index) {
		clients.remove(index);
		return this;
	}

	public int indexOf(String id) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).id.equals(id))
				return i;
		return -1;
	}

	public int indexOf(T client) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).client.equals(client))
				return i;
		return -1;
	}

	public T getClient(int index) {
		if(index>clients.size() || index<0) return null;
		return clients.get(index).client;
	}

	public int size() {
		return clients.size();
	}

	public T getLeftNeighborOf(int index) {
		return index == 0 ? clients.get(clients.size() - 1).client : clients.get(index - 1).client;
	}

	public T getRightNeighborOf(int index) {
		return index < clients.size() - 1 ? clients.get(index + 1).client : clients.get(0).client;
	}

	public T getLeftNeighborOf(T t) {
		return getLeftNeighborOf(indexOf(t));
	}

	public T getRightNeighborOf(T t) {
		return getRightNeighborOf(indexOf(t));
	}

	public void setTimestamp(int index,Timestamp timestamp){
		clients.get(index).setTimestamp(timestamp);
	}

	public void removeOld(){
		clients.removeIf(client -> {
			Timestamp cTime = new Timestamp(Calendar.getInstance().getTime().getTime());
			long timeDifference = cTime.getTime() - client.timestamp.getTime();

			return timeDifference>Broker.LEASE_DURATION;
		});
	}

}
