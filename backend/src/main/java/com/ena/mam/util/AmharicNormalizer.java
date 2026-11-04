package com.ena.mam.util;

public final class AmharicNormalizer {

    private AmharicNormalizer() {}

    public static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String t = text;

        // These MUST run in the exact inside-out order of the SQL amh_norm() function
        t = translate(t, "ሐሑሒሓሔሕሖ", "ሀሁሂሃሄህሆ");      // 1. ሐ-row -> ሀ-row
        t = translate(t, "ኀኁኂኃኄኅኆ", "ሀሁሂሃሄህሆ");      // 2. ኀ-row -> ሀ-row
        t = translate(t, "ሃ", "ሀ");                     // 3. 4th order ሃ -> ሀ (Catches original + steps 1 & 2)
        t = translate(t, "ሗኇ", "ኋሇ");                    // 4. labialized forms
        t = translate(t, "ሠሡሢሣሤሥሦሧ", "ሰሱሲሳሴስሶሷ");   // 5. ሠ-row -> ሰ-row
        t = translate(t, "ዐዑዒዓዔዕዖ", "አኡኢኣኤእኦ");      // 6. ዐ-row -> አ-row
        t = translate(t, "ኣ", "አ");                     // 7. 4th order ኣ -> አ (Catches original + step 6)
        t = translate(t, "ፀፁፂፃፄፅፆፇ", "ጸጹጺጻጼጽጾጿ");      // 8. ፀ-row -> ጸ-row

        return t;
    }

    /**
     * Perfectly mimics PostgreSQL's translate(string, from, to) function.
     */
    private static String translate(String text, String from, String to) {
        StringBuilder sb = new StringBuilder(text.length());

        for (char c : text.toCharArray()) {
            int idx = from.indexOf(c);
            if (idx != -1) {
                sb.append(to.charAt(idx));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}