package aqua.blatt1.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SnapshotController implements ActionListener {
	private final TankModel tm;


	public SnapshotController(TankModel tm) {
		this.tm = tm;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Runnable iss = new Runnable() {
			@Override
			public void run() {
				tm.initiateSnapshot();
			}
		};
		iss.run();

	}
}
