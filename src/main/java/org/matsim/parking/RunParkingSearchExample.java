package org.matsim.parking;

import java.nio.file.Path;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpGlobalRoutingNetworkProvider;
import org.matsim.contrib.dvrp.router.DvrpModeRoutingModule;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingListener;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingSearchEvaluator;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingSlotVisualiser;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.WalkLegFactory;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.NoVehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationToNearbyParking;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.routing.WithinDayParkingRouter;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchPopulationModule;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchQSimModule;
import org.matsim.contrib.parking.parkingsearch.sim.SetupParking;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Key;
import com.google.inject.name.Names;

public class RunParkingSearchExample {

	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig("scenarios/parking/parkingsearch/config1.xml",
				new ParkingSearchConfigGroup());
		// all further input files are set in the config.

		// get the parking search config group to set some parameters, like agent's
		// search strategy or average parking slot length
		ParkingSearchConfigGroup configGroup = (ParkingSearchConfigGroup) config.getModules()
				.get(ParkingSearchConfigGroup.GROUP_NAME);
		configGroup.setParkingSearchStrategy(ParkingSearchStrategy.Random);
		config.controler().setOutputDirectory(Path.of(config.controler().getOutputDirectory())
				.resolve("Parkingsearch_" + java.time.LocalDate.now().toString() + "_"
						+ java.time.LocalTime.now().toSecondOfDay() + "_" + configGroup.getParkingSearchStrategy())
				.toString());
		// set to false, if you don't require visualisation, then the example will run
		// for 10 iterations, with OTFVis, only one iteration is performed.
		boolean otfvis = false;
		if (otfvis) {
			config.controler().setLastIteration(0);
		} else {
			config.controler().setLastIteration(0);
		}
		new RunParkingSearchExample().run(config, otfvis);

	}

	/**
	 * @param config a standard MATSim config
	 * @param otfvis turns otfvis visualisation on or off
	 */
	public void run(Config config, boolean otfvis) {
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		config.qsim().setSnapshotStyle(SnapshotStyle.kinematicWaves);

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				ParkingSlotVisualiser visualiser = new ParkingSlotVisualiser(scenario);
				addEventHandlerBinding().toInstance(visualiser);
				addControlerListenerBinding().toInstance(visualiser);
			}
		});
		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		SetupParking.installParkingModules(controler);
//		installParkingModules(controler);
		controler.run();
	}

	private void installParkingModules(Controler controler) {
		// No need to route car routes in Routing module in advance, as they are
				// calculated on the fly
				if (!controler.getConfig().getModules().containsKey(DvrpConfigGroup.GROUP_NAME)) {
					controler.getConfig().addModule(new DvrpConfigGroup());
				}

				controler.addOverridingModule(new DvrpTravelTimeModule());
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						bind(TravelTime.class).annotatedWith(DvrpModes.mode(TransportMode.car))
								.to(Key.get(TravelTime.class, Names.named(DvrpTravelTimeModule.DVRP_ESTIMATED)));
						bind(TravelDisutilityFactory.class).annotatedWith(DvrpModes.mode(TransportMode.car))
								.toInstance(TimeAsTravelDisutility::new);
						bind(Network.class).annotatedWith(DvrpModes.mode(TransportMode.car))
								.to(Key.get(Network.class, Names.named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING)));
						install(new DvrpModeRoutingModule(TransportMode.car, new SpeedyALTFactory()));
						bind(Network.class).annotatedWith(Names.named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING))
								.to(Network.class)
								.asEagerSingleton();
						bind(ParkingSearchManager.class).to(FacilityBasedParkingManager.class).asEagerSingleton();
						this.install(new ParkingSearchQSimModule());
						addControlerListenerBinding().to(ParkingListener.class);
						bind(ParkingRouter.class).to(WithinDayParkingRouter.class);
						bind(VehicleTeleportationLogic.class).to(NoVehicleTeleportationLogic.class);
//						bind(VehicleTeleportationLogic.class).to(VehicleTeleportationToNearbyParking.class);
					}
				});

				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						QSimComponentsConfig components = new QSimComponentsConfig();

						new StandardQSimComponentConfigurator(controler.getConfig()).configure(components);
						components.removeNamedComponent(PopulationModule.COMPONENT_NAME);
						components.addNamedComponent(ParkingSearchPopulationModule.COMPONENT_NAME);

						bind(QSimComponentsConfig.class).toInstance(components);
					}
				});

			
	}

}
