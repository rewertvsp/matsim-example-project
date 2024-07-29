package org.matsim.analysis.emissions;

public class RunEmissions {

	private enum HbefaMethod {average, detailed}
	
	public static void main(String[] args) {
//		String runDirectory = "C:/Users/Ricardo/Desktop/SoundingBoard/commercialTraffic_1pct_2023-02-28_44636_0.005/";
//		String runDirectory = "C:/Users/Ricardo/Desktop/SoundingBoard/commercialTraffic_25pct_2023-02-28_43771_0.005/";
//		String runDirectory = "C:/Users/Ricardo/Desktop/SoundingBoard/freightTraffic_1pct_2023-02-28_43074_0.005/";
//		String runDirectory = "C:/Users/Ricardo/Desktop/SoundingBoard/freightTraffic_1pct_2023-03-02_58938_0.005/";
//		String runDirectory = "C:/Users/Ricardo/Desktop/SoundingBoard/freightTraffic_10pct_2023-02-28_46047_0.005/";
		String runDirectory = "C:/Users/Ricardo/Desktop/SoundingBoard/freightTraffic_20pct_2023-03-09_40007_0.005/";
//		String runDirectory = "C:/Users/Ricardo/Desktop/SoundingBoard/freightTraffic_1pct_2023-03-02_58938_0.005/";
		
		String runId = "base";
//		String runId = "tax40CV";
		HbefaMethod selectedHbefaMethod = HbefaMethod.detailed; 
		RunOfflineAirPollutionAnalysisByVehicleCategory.main(new String[] { runDirectory, runId, selectedHbefaMethod.toString()});

	}
}
