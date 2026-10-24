# PhishGuard

Checks a link or an email and tells you whether it looks like phishing.

Built for **ACM SYNAPSE 2026** (DITU ACM SC) - Problem Statement ACM_PS3: Phishing Link and Email Checker.

By: **TEAM NEURAL REAPERS** | DIT UNIVERSITY

## What it does

- **Link checker:** scores a URL from 0 to 100 and shows every reason behind the score.
- **Email / SMS checker:** looks at urgent wording, requests for passwords / OTP / card details,
  generic greetings, prize bait, the sender's address and every link inside the message.
- **Verdict:** LIKELY SAFE (0-29), SUSPICIOUS (30-59) or HIGH RISK (60-100).
- **Made with Indian scams in mind:** KYC expiry, UPI/OTP requests, fake SBI / HDFC / Paytm pages.

## How the checks work

Each check adds points and a plain-language reason. Examples:

| Check | Example |
|---|---|
| Lookalike domain (1 -> l, 0 -> o) | `paypa1-secure-login.xyz` |
| IP address instead of a name | `http://192.168.10.5/bank/login.php` |
| `@` trick | `http://google.com@login-verify.xyz/` |
| Link shortener | `https://bit.ly/...` |
| Brand name in an unofficial domain | `sbi-online.top` |
| Scam words / risky endings | `kyc`, `refund`, `.xyz`, `.top` |
| Urgent or threatening email text | "your account will be blocked" |

A small allowlist stops real sites (like `google-analytics.com`) from being flagged by mistake.

## Run it (Windows / Mac / Linux)

You need **JDK 17 or newer** (`java -version` to check). No other libraries are needed.

```
javac *.java
java WebApp
```

Then open **http://localhost:8080** in your browser.

Terminal version (no browser): `java PhishGuard`

## Test results

`java AccuracyTest` reads `urls.txt` (links I labelled `good` or `bad`):

- Phishing links caught: **13 / 15**
- Safe links wrongly flagged: **0 / 16**

This is a small test set I collected myself, so treat it as a sanity check, not a scientific accuracy figure.
Sample links and emails for a demo are in `demo_samples.txt`.

## Known limitations

- `https://goggle.com` (a plain typo of google) is not caught.
- `https://paypal.com.secure-check.xyz` (real brand used as a decoy) is not caught.
- Pure social-engineering messages with no links or keywords (for example "I lost my wallet, send money") pass through.
- Rules are fixed, so brand-new scam wording must be added by hand.

## Future work

Typo detection with edit distance, a browser extension, WhatsApp / SMS scanning, a reputation-API check,
and a machine-learning score on top of the rules.

## Files

| File | Purpose |
|---|---|
| `PhishGuard.java` | The checking logic (links + emails) and a terminal menu |
| `WebApp.java` | Tiny web server that connects the page to the checker |
| `index.html` | The web page |
| `AccuracyTest.java` + `urls.txt` | Runs the checker on labelled links and prints the results |
| `demo_samples.txt` | Links and emails to try, with expected results |

## Development notes

I used an AI assistant for guidance while learning to build this, which the coding round allows.
I typed, ran, tested and debugged the code myself, collected the test links, and added the
India-specific scam checks.