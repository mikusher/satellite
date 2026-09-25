import com.mikusher.logger.factory.Logger;
import com.mikusher.logger.factory.LoggerFactory;
import com.mikusher.parameter.SatelliteData;

public class App {
    public static void test() {
        LoggerFactory.setIncludeLoggerName(false);
        LoggerFactory.setDateFormatString("yyyy-MM-dd HH:mm:ss.SSS");
        LoggerFactory.setJustJsonLogger(true);

        Logger logger = LoggerFactory.getLogger(App.class);

        SatelliteData data = new SatelliteData();
        data.put("CaseNumber", "C12.12343");
        data.put("Step", "Assignment");
        data.put("Department", "BPM");

        String lob = "Container";
        String service = "Transport";
        int process = 10;
        String reason = "Connection";

        logger.error().message("Confirm message 'incompatible Data in document'")
                .setString("LOB", lob)
                .field("Service", service)
                .setInteger("NumProcess", process)
                .field("Reason", reason)
                .map("OptionalFields", data)
                .log();

        logger.debug().message("Confirm message 'incompatible Data in document'")
                .field("LOB", lob)
                .field("Service", service)
                .setInteger("Process", process)
                .field("Reason", reason)
                .map("OptionalFields", data)
                .log();
    }

    public static void main(String[] args) {
        test();
    }
}
