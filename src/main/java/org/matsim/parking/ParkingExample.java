package org.matsim.parking;

import org.matsim.contrib.parking.parkingchoice.run.RunParkingChoiceExample;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

public class ParkingExample {
	public final void testRun() {
		Config config = ConfigUtils.loadConfig("./src/main/resources/parkingchoice/config.xml");
		config.controler().setOutputDirectory("output");
		config.controler().setLastIteration(0);
		RunParkingChoiceExample.run(config);
		
	}
}
