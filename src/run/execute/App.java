import com.mikusher.logger.factory.Logger;
import com.mikusher.logger.factory.LoggerFactory;
import com.mikusher.parameter.ParameterMap;


public class App {
    public static void Test() {

        LoggerFactory.setIncludeLoggerName(false);
        LoggerFactory.setDateFormatString("yyyy-MM-dd HH:mm:ss.SSS");
        LoggerFactory.setJustJsonLogger(true);

        Logger _loggerSatellite = LoggerFactory.getLogger(App.class);

        ParameterMap pMap = new ParameterMap();
        pMap.put("CaseNumber", "C12.12343");
        pMap.put("Step", "Assignment");
        pMap.put("Department", "BPM");

        String LOB = "Container";
        String Service = "Transport";
        String Process = "Add";
        String Reason = "Connection";

        _loggerSatellite.error().message("Confirm message 'incompatible Data in document'")
                .field("LOB", LOB)
                .field("Service", Service)
                .field("Process", Process)
                .field("Reason", Reason)
                .map("OptionalFields", pMap).log();

        _loggerSatellite.debug().message("Confirm message 'incompatible Data in document'")
                .field("LOB", LOB)
                .field("Service", Service)
                .field("Process", Process)
                .field("Reason", Reason)
                .map("OptionalFields", pMap).log();
    }


    public static void main(String[] args) {
        Test();
    }
}
