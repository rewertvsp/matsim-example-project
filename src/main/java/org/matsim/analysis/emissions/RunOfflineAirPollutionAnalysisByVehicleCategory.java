package org.matsim.analysis.emissions;
/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.VspHbefaRoadTypeMapping;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.matsim.application.ApplicationUtils.globFile;
import static org.matsim.contrib.emissions.Pollutant.*;

/**
 * This analysis class requires two parameters as arguments: <br>
 * (1) the run directory, and <br>
 * (2) the password (passed as environment variable in your IDE
 * and/or on the server) to access the encrypted files on the public-svn.
 *
 * @author
*/

public class RunOfflineAirPollutionAnalysisByVehicleCategory implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(RunOfflineAirPollutionAnalysisByVehicleCategory.class);
	private final String runDirectory;
	private final String runId;
	private final String hbefaAverageFileWarm;
	private final String hbefaAverageColdFile;
	private final String hbefaDetailedFileWarm;
	private final String hbefaDetailedColdFile;
	private final String analysisOutputDirectory;
	static List<Pollutant> pollutants2Output = Arrays.asList(CO2_TOTAL, NOx, PM, PM_non_exhaust, FC);
	
	public RunOfflineAirPollutionAnalysisByVehicleCategory(String runDirectory, String runId, String hbefaAverageFileWarm, String hbefaAverageColdFile, String hbefaDetailedFileWarm, String hbefaDetailedColdFile, String analysisOutputDirectory) {
		this.runDirectory = runDirectory;
		this.runId = runId;
		this.hbefaAverageFileWarm = hbefaAverageFileWarm;
		this.hbefaAverageColdFile = hbefaAverageColdFile;
		this.hbefaDetailedFileWarm = hbefaDetailedFileWarm;
		this.hbefaDetailedColdFile = hbefaDetailedColdFile;
		
		if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";
		this.analysisOutputDirectory = analysisOutputDirectory;
	}
	
	public static void main(String[] args) {

		if (args.length == 3) {
			String runDirectory = args[0];

			if (!runDirectory.endsWith("/"))
				runDirectory = runDirectory + "/";

			final String runId = args[1]; // based on the simulation output available in this project
			final String hbefaPath = "../public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";
			final String selectedHbefaMethod = args[2];
			
			String hbefaAverageFileWarm = null;
			String hbefaAverageColdFile = null;
			String hbefaDetailedFileWarm = null;
			String hbefaDetailedColdFile = null;
			
			if (selectedHbefaMethod.equals("average")) {
			hbefaAverageFileWarm = hbefaPath + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";
			hbefaAverageColdFile = hbefaPath + "ColdStart_Vehcat_2020_Average_withHGVetc.csv.enc";
			}
			else if (selectedHbefaMethod.equals("detailed")) {
				hbefaAverageFileWarm = hbefaPath + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";
				hbefaAverageColdFile = hbefaPath + "ColdStart_Vehcat_2020_Average_withHGVetc.csv.enc";
				hbefaDetailedFileWarm = hbefaPath + "944637571c833ddcf1d0dfcccb59838509f397e6.enc";
				hbefaDetailedColdFile = hbefaPath + "54adsdas478ss457erhzj5415476dsrtzu.enc";
			}
			RunOfflineAirPollutionAnalysisByVehicleCategory analysis = new RunOfflineAirPollutionAnalysisByVehicleCategory(
					runDirectory, runId, hbefaAverageFileWarm, hbefaAverageColdFile, hbefaDetailedFileWarm, hbefaDetailedColdFile, runDirectory + "emission_analysis");
			try {
				analysis.call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		} else {
			throw new RuntimeException(
					"Please set the run directory path and/or password. \nCheck the class description for more details. Aborting...");
		}
	}

	public Integer call() throws Exception {

		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(String.valueOf(globFile(Path.of(runDirectory), runId, "output_allVehicles")));
		config.network().setInputFile(String.valueOf(globFile(Path.of(runDirectory), runId, "network")));
		//config.transit().setTransitScheduleFile(String.valueOf(globFile(Path.of(runDirectory), runId, "transitSchedule")));
//		config.transit().setVehiclesFile(String.valueOf(globFile(Path.of(runDirectory), runId, "transitVehicles")));

		config.global().setCoordinateSystem(TransformationFactory.DHDN_GK4);
		log.info("Using coordinate system '{}'", config.global().getCoordinateSystem());
		config.plans().setInputFile(null);
		config.parallelEventHandling().setNumberOfThreads(null);
		config.parallelEventHandling().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(4);
		
		EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
		eConfig.setDetailedVsAverageLookupBehavior(DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
		eConfig.setAverageColdEmissionFactorsFile(this.hbefaAverageColdFile);
		eConfig.setAverageWarmEmissionFactorsFile(this.hbefaAverageFileWarm);
		eConfig.setDetailedWarmEmissionFactorsFile(this.hbefaDetailedFileWarm);
		eConfig.setDetailedColdEmissionFactorsFile(this.hbefaDetailedColdFile);
		eConfig.setNonScenarioVehicles(NonScenarioVehicles.abort);
		eConfig.setHbefaTableConsistencyCheckingLevel(HbefaTableConsistencyCheckingLevel.consistent);

		// input and outputs of emissions analysis
		final String eventsFile = globFile(Path.of(runDirectory), runId, "output_events");
		File dir = new File(analysisOutputDirectory);
		if ( !dir.exists() ) { dir.mkdir(); }
		final String emissionEventOutputFile = analysisOutputDirectory + runId + ".emission.events.offline.xml.gz";
		log.info("Writing emissions (link totals) to: {}", emissionEventOutputFile);
		// for SimWrapper
		final String linkEmissionPerMOutputFile = analysisOutputDirectory + runId + ".emissionsPerLinkPerM.csv";
		log.info("Writing emissions per link [g/m] to: {}", linkEmissionPerMOutputFile);
		final String linkEmissionOutputFile = analysisOutputDirectory + runId + ".emissionsPerLink.csv";
		log.info("Writing emissions to: {}", linkEmissionOutputFile);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// network
		new VspHbefaRoadTypeMapping().addHbefaMappings(scenario.getNetwork());
		log.info("Using integrated road types");

        EventsManager eventsManager = EventsUtils.createEventsManager();

		AbstractModule module = new AbstractModule(){
			@Override
			public void install(){
				bind( Scenario.class ).toInstance( scenario );
				bind( EventsManager.class ).toInstance( eventsManager );
				bind( EmissionModule.class ) ;
			}
		};

		com.google.inject.Injector injector = Injector.createInjector(config, module);

        EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

        EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

		// necessary for link emissions [g/m] output
        EmissionsOnLinkHandler emissionsOnLinkEventHandler = new EmissionsOnLinkHandler();
		eventsManager.addHandler(emissionsOnLinkEventHandler);

        eventsManager.initProcessing();
        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(eventsFile);
		log.info("-------------------------------------------------");
		log.info("Done reading the events file");
		log.info("Finish processing...");
		eventsManager.finishProcessing();
		log.info("Closing events file...");
        emissionEventWriter.closeFile();
		log.info("Done");
		log.info("Writing (more) output...");

		{ // writing emissions (per link) per meter and as absolute volumes per link
			
			File linkEmissionPerMAnalysisFile = new File(linkEmissionPerMOutputFile);
			File linkEmissionAnalysisFile = new File(linkEmissionOutputFile);
			
			BufferedWriter absolutWriter  = new BufferedWriter(new FileWriter(linkEmissionAnalysisFile));
			BufferedWriter perMeterWriter = new BufferedWriter(new FileWriter(linkEmissionPerMAnalysisFile));
			
			absolutWriter.write("linkId");
			perMeterWriter.write("linkId");
			
			for (Pollutant pollutant : pollutants2Output) {
				absolutWriter.write(";" + pollutant);
				perMeterWriter.write(";" + pollutant + " [g/m]");
			}
			absolutWriter.newLine();
			perMeterWriter.newLine();

			Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsOnLinkEventHandler.getLink2pollutants();
			Object2DoubleMap<Pollutant> totalPollutants = new Object2DoubleOpenHashMap<Pollutant>();
			for (Id<Link> linkId : link2pollutants.keySet()) {
				absolutWriter.write(linkId.toString());
				perMeterWriter.write(linkId.toString());

				for (Pollutant pollutant : pollutants2Output) {
					double emissionValue = 0.;
					if (link2pollutants.get(linkId).get(pollutant) != null) {
						emissionValue = link2pollutants.get(linkId).get(pollutant);
					}
					totalPollutants.mergeDouble(pollutant, emissionValue, Double::sum);
					absolutWriter.write(";" + emissionValue);
					double emissionPerM = Double.NaN;
					Link link = scenario.getNetwork().getLinks().get(linkId);
					if (link != null) {
						emissionPerM = emissionValue / link.getLength();
					}

					perMeterWriter.write(";" + emissionPerM);
				}
				absolutWriter.newLine();
				perMeterWriter.newLine();
			}
			absolutWriter.write("Sum");
			for (Pollutant pollutant : pollutants2Output) {
				absolutWriter.write(";" + totalPollutants.getDouble(pollutant));
			}
			absolutWriter.newLine();
			perMeterWriter.close();
			absolutWriter.close();

			log.info("Done");
			log.info("All output written to " + analysisOutputDirectory);
			log.info("-------------------------------------------------");
		}

		return 0;
	}
}
