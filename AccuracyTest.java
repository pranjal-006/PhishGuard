import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Reads urls.txt. Each line looks like:  good|https://www.google.com
//                                    or:  bad|http://paypa1-login.xyz
// Runs every URL through PhishGuard and shows how many it got right.
public class AccuracyTest {

    public static void main(String[] args) throws Exception {
        int goodTotal = 0;
        int badTotal = 0;
        int badCaught = 0;
        int falseAlarms = 0;
        List<String> missed = new ArrayList<>();
        List<String> alarms = new ArrayList<>();

        for (String line : Files.readAllLines(Path.of("urls.txt"))) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || !line.contains("|")) {
                continue;
            }
            String label = line.substring(0, line.indexOf("|"));
            String url = line.substring(line.indexOf("|") + 1).trim();

            List<String> reasons = new ArrayList<>();
            int score = PhishGuard.analyzeUrl(url, reasons);
            boolean flagged = score >= 30;   // SUSPICIOUS or HIGH RISK counts as flagged

            if (label.equals("bad")) {
                badTotal++;
                if (flagged) {
                    badCaught++;
                } else {
                    missed.add(url + "  (score " + score + ")");
                }
            } else {
                goodTotal++;
                if (flagged) {
                    falseAlarms++;
                    alarms.add(url + "  (score " + score + ")");
                }
            }
        }

        System.out.println("Phishing links caught: " + badCaught + " / " + badTotal);
        System.out.println("Safe links wrongly flagged: " + falseAlarms + " / " + goodTotal);
        int correct = badCaught + (goodTotal - falseAlarms);
        int total = badTotal + goodTotal;
        System.out.println("Overall correct: " + correct + " / " + total
                + " (" + (100 * correct / Math.max(total, 1)) + "%)");

        if (!missed.isEmpty()) {
            System.out.println("\nMISSED (phishing rated safe):");
            for (String m : missed) {
                System.out.println("  " + m);
            }
        }
        if (!alarms.isEmpty()) {
            System.out.println("\nFALSE ALARMS (safe rated risky):");
            for (String a : alarms) {
                System.out.println("  " + a);
            }
        }
    }
}