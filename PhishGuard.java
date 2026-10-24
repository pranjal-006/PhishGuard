import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PhishGuard {

    static final String[] BRANDS = {"paypal", "google", "amazon", "microsoft", "apple",
            "facebook", "netflix", "instagram", "whatsapp", "paytm", "flipkart",
            "sbi", "hdfcbank", "icicibank", "irctc", "phonepe", "uidai", "epfo"};

    // Gets just the website part, e.g. "paypa1-secure-login.xyz"
    static String getHost(String url) {
        String s = url.trim().toLowerCase();
        if (s.contains("://")) {
            s = s.substring(s.indexOf("://") + 3);
        }
        int slash = s.indexOf("/");
        if (slash != -1) {
            s = s.substring(0, slash);
        }
        if (s.contains("@")) {
            s = s.substring(s.lastIndexOf("@") + 1);
        }
        int colon = s.indexOf(":");
        if (colon != -1) {
            s = s.substring(0, colon);
        }
        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    // Turns fake letters back: paypa1 -> paypal
    static String unswap(String s) {
        return s.replace("0", "o").replace("1", "l").replace("3", "e")
                .replace("4", "a").replace("5", "s");
    }

    static final String[] OFFICIAL = {"google-analytics.com"};

    // Does this website name imitate a famous brand? Returns points to add.
    static int brandCheck(String host, List<String> reasons) {
        int points = 0;
                for (String ok : OFFICIAL) {
            if (host.equals(ok) || host.endsWith("." + ok)) {
                return 0;
            }
        }
        String[] parts = host.split("\\.");
        if (parts.length >= 2) {
            String name = parts[parts.length - 2];
            for (String piece : name.split("-")) {
                for (String brand : BRANDS) {
                    if (!piece.equals(brand) && unswap(piece).equals(brand)) {
                        points += 40;
                        reasons.add("Lookalike of '" + brand + "': '" + piece + "'");
                    } else if (piece.equals(brand) && !name.equals(brand)) {
                        points += 30;
                        reasons.add("Uses the brand name '" + brand + "' in an unofficial domain");
                    }
                }
            }
        }
        return points;
    }

    // Scores one URL. Reasons are added to the list you pass in.
    static int analyzeUrl(String url, List<String> reasons) {
        int score = 0;
                String[] shorteners = {"bit.ly", "tinyurl.com", "t.co", "goo.gl", "cutt.ly", "is.gd"};
        for (String sh : shorteners) {
            if (getHost(url).equals(sh)) {
                score += 30;
                reasons.add("URL shortener hides the real destination");
            }
        }
        String lower = url.toLowerCase();

        if (lower.startsWith("http://")) {
            score += 15;
            reasons.add("Not using HTTPS");
        }
        if (lower.contains("@")) {
            score += 30;
            reasons.add("Contains @ symbol");
        }
        if (lower.length() > 75) {
            score += 10;
            reasons.add("Very long URL");
        }
        if (lower.matches("^https?://\\d+\\.\\d+\\.\\d+\\.\\d+.*")) {
            score += 35;
            reasons.add("Uses an IP address instead of a domain name");
        }

                    String[] words = {"login", "verify", "update", "secure", "bank", "account", "kyc", "refund", "claim", "free", "gift"};
        for (String w : words) {
            if (lower.contains(w)) {
                score += 8;
                reasons.add("Suspicious word: " + w);
            }
        }

                String[] badEndings = {".xyz", ".top", ".tk", ".work", ".click", ".icu", ".buzz"};
        for (String e : badEndings) {
            if (lower.contains(e)) {
                score += 15;
                reasons.add("Suspicious domain ending: " + e);
            }
        }

        score += brandCheck(getHost(lower), reasons);
        return score;
    }

    static String verdictFor(int score) {
        if (score >= 60) {
            return "HIGH RISK";
        } else if (score >= 30) {
            return "SUSPICIOUS";
        } else {
            return "LIKELY SAFE";
        }
    }

    static void printResult(int score, List<String> reasons) {
        if (score > 100) {
            score = 100;
        }
        System.out.println("Verdict: " + verdictFor(score) + " (score " + score + ")");
        for (String r : reasons) {
            System.out.println("  - " + r);
        }
    }

    static void checkUrl(String url) {
        List<String> reasons = new ArrayList<>();
        int score = analyzeUrl(url, reasons);
        printResult(score, reasons);
    }

    // ---------------- NEW: email checking ----------------

        static int analyzeEmail(String sender, String text, List<String> reasons) {
        int score = 0;
        String lower = text.toLowerCase();

        String[] urgent = {"act now", "immediately", "urgent", "within 24 hours",
                "suspended", "verify your account", "final notice", "unauthorized",
                "blocked", "kyc", "expired", "update now"};
        for (String w : urgent) {
            if (lower.contains(w)) {
                score += 8;
                reasons.add("Urgent or threatening language: '" + w + "'");
            }
        }

        String[] sensitive = {"password", "otp", "cvv", "card number", "your pin",
                "aadhaar", "bank details", "upi pin", "pan card"};
        for (String w : sensitive) {
            if (lower.contains(w)) {
                score += 12;
                reasons.add("Asks for sensitive information: '" + w.trim() + "'");
            }
        }

        String[] generic = {"dear customer", "dear user", "dear account holder"};
        for (String w : generic) {
            if (lower.contains(w)) {
                score += 8;
                reasons.add("Generic greeting instead of your name");
            }
        }

        String[] prizes = {"you have won", "lottery", "claim your prize", "cashback", "refund"};
        for (String w : prizes) {
            if (lower.contains(w)) {
                score += 12;
                reasons.add("Prize bait: '" + w + "'");
            }
        }

        // Check the sender's email address
        if (sender.contains("@")) {
            String senderHost = sender.substring(sender.lastIndexOf("@") + 1)
                    .replace(">", "").trim().toLowerCase();
            List<String> senderReasons = new ArrayList<>();
            int s = brandCheck(senderHost, senderReasons);
            score += s;
            for (String r : senderReasons) {
                reasons.add("Sender address: " + r);
            }
        }

        // Check every link inside the email
        for (String word : text.split("\\s+")) {
            if (word.toLowerCase().startsWith("http")) {
                String link = word.replaceAll("[.,;)\"'>]+$", "");
                List<String> linkReasons = new ArrayList<>();
                int s = analyzeUrl(link, linkReasons);
                if (s >= 60) {
                    score += 25;
                    reasons.add("Contains a high-risk link: " + link);
                } else if (s >= 30) {
                    score += 10;
                    reasons.add("Contains a suspicious link: " + link);
                }
            }
        }

               return score;
    }

    static void checkEmail(String sender, String text) {
        List<String> reasons = new ArrayList<>();
        int score = analyzeEmail(sender, text, reasons);
        if (reasons.isEmpty()) {
            reasons.add("No suspicious signs found");
        }
        printResult(score, reasons);
    }

    // Reads many lines until the user types END
    static String readEmail(Scanner sc) {
        System.out.println("Paste the email text. Type END on a new line when finished:");
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = sc.nextLine();
            if (line.trim().equals("END")) {
                break;
            }
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("\n1 = check a URL, 2 = check an email, 0 = exit: ");
            String choice = sc.nextLine().trim();
            if (choice.equals("0")) {
                break;
            } else if (choice.equals("1")) {
                System.out.print("Enter a URL: ");
                checkUrl(sc.nextLine().trim());
            } else if (choice.equals("2")) {
                System.out.print("Sender (e.g. PayPal <help@paypa1-alerts.top>), or press Enter to skip: ");
                String sender = sc.nextLine().trim();
                String text = readEmail(sc);
                checkEmail(sender, text);
            } else {
                System.out.println("Please type 1, 2 or 0.");
            }
        }
    }
}
