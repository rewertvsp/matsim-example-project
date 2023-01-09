package org.matsim.parking;

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.multimodal.router.util.WalkTravelTime;
import org.matsim.contrib.parking.parkingchoice.PC2.GeneralParkingModule;
import org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PublicParking;
import org.matsim.contrib.parking.parkingchoice.PC2.scoring.ParkingScore;
import org.matsim.contrib.parking.parkingchoice.PC2.scoring.ParkingScoreManager;
import org.matsim.contrib.parking.parkingchoice.PC2.simulation.ParkingInfrastructure;
import org.matsim.contrib.parking.parkingchoice.PC2.simulation.ParkingInfrastructureManager;
import org.matsim.contrib.parking.parkingchoice.example.ParkingBetaExample;
import org.matsim.contrib.parking.parkingchoice.example.ParkingCostCalculatorExample;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.scenario.ScenarioUtils;

public class ParkingChoiceExample {
	
	
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("scenarios/parking/parkingchoice/config_new.xml");
		config.controler().setOutputDirectory("output/parkingChoice/");
		config.controler().setLastIteration(0);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );

		Population population = scenario.getPopulation();
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {

				for (Activity activity : PopulationUtils.getActivities(plan,
						StageActivityHandling.ExcludeStageActivities)) {
					if (activity.getCoord() == null)
						activity.setCoord(scenario.getNetwork().getLinks().get(activity.getLinkId()).getCoord());
				}
			}
		}
		// we need some settings to walk from parking to destination:
		ParkingScore parkingScoreManager = new ParkingScoreManager(new WalkTravelTime(controler.getConfig().plansCalcRoute()), scenario);
		parkingScoreManager.setParkingScoreScalingFactor(1);
		parkingScoreManager.setParkingBetas(new ParkingBetaExample());

		// ---

		ParkingInfrastructure parkingInfrastructureManager = new ParkingInfrastructureManager(parkingScoreManager, controler.getEvents());
		{
			LinkedList<PublicParking> publicParkings = new LinkedList<PublicParking>();
			//parking 1: we place this near the workplace
			publicParkings.add(new PublicParking(Id.create("workPark", PC2Parking.class), 98, new Coord((double) 0, (double) -75),
					new ParkingCostCalculatorExample(1), "park"));
			//parking 2: we place this at home
//			final double x = -25000;
			publicParkings.add(new PublicParking(Id.create("homePark", PC2Parking.class), 98, new Coord((double) -700, (double) -800),
					new ParkingCostCalculatorExample(0), "park"));
			parkingInfrastructureManager.setPublicParkings(publicParkings);
		}


		//setting up the Parking Module
		GeneralParkingModule generalParkingModule = new GeneralParkingModule(controler);
		generalParkingModule.setParkingScoreManager(parkingScoreManager);
		generalParkingModule.setParkingInfrastructurManager(parkingInfrastructureManager);

		controler.run();
	}
}
