package com.example.rental.service.ai;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Centralised, robust date parser used by the AI layer.
 *
 * <p>Accepts (in order of preference):
 * <ul>
 *   <li>ISO {@code yyyy-MM-dd} -- the canonical wire format. Spring/Jackson
 *       already uses this for {@code LocalDate} fields.</li>
 *   <li>ISO {@code yyyy-MM} (interpreted as the first day of that month).</li>
 *   <li>Vietnamese-friendly {@code dd/MM/yyyy} and {@code MM/yyyy}.</li>
 *   <li>Natural-language Vietnamese hints such as "thang 7 nam nay",
 *       "thang sau", "thang truoc", "nam nay", "quy nay", "hom nay".</li>
 * </ul>
 *
 * <p>Never hard-codes the year. All "this year" / "next month" / "this quarter"
 * are resolved against {@link LocalDate#now()} at call time, so the result
 * always matches the real current date regardless of when the model emits
 * a stale example.</p>
 */
public final class DateTimeNormalizer {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter VN_DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter VN_MMYYYY = DateTimeFormatter.ofPattern("MM/yyyy");

    private static final Pattern P_YYYY_MM_DD = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern P_YYYY_MM = Pattern.compile("^\\d{4}-\\d{1,2}$");
    private static final Pattern P_DD_MM_YYYY = Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}$");
    private static final Pattern P_MM_YYYY = Pattern.compile("^\\d{1,2}/\\d{4}$");

    private static final Pattern P_NL_THANG_NAY = Pattern.compile(
            "(th[aá]ng\\s*(n[aá]y|hi[eệ]n\\s*t[aạ]i))", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_NL_THANG_SAU = Pattern.compile(
            "th[aá]ng\\s*sau", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_NL_THANG_TRUOC = Pattern.compile(
            "th[aá]ng\\s*tr[uư][oơ]c", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_NL_THANG_X = Pattern.compile(
            "th[aá]ng\\s*(\\d{1,2})(?:\\s*nam\\s*(n[aá]y|hi[eệ]n\\s*t[aạ]i))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_NL_NAM_NAY = Pattern.compile(
            "n[aă]m\\s*(n[aá]y|hi[eệ]n\\s*t[aạ]i)", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_NL_QUY_NAY = Pattern.compile(
            "qu[yý]\\s*(n[aá]y|hi[eệ]n\\s*t[aạ]i)", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_NL_HOM_NAY = Pattern.compile(
            "h[oô]m\\s*n[aá]y", Pattern.CASE_INSENSITIVE);

    private DateTimeNormalizer() {}

    /**
     * Parses a free-form date string. Returns the first day of the month
     * for month-only inputs ({@code yyyy-MM} or {@code MM/yyyy}).
     *
     * @throws IllegalArgumentException if the input cannot be parsed.
     */
    public static LocalDate parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Thieu ngay");
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Thieu ngay");
        }

        LocalDate today = LocalDate.now();

        // 1. Natural-language hints first, since they contain no parseable date token
        //    on their own and the LLM may emit them alongside a year.
        String norm = stripAccents(s).toLowerCase(Locale.ROOT);

        if (P_NL_HOM_NAY.matcher(norm).find()) {
            return today;
        }
        if (P_NL_THANG_TRUOC.matcher(norm).find()) {
            return today.minusMonths(1).withDayOfMonth(1);
        }
        if (P_NL_THANG_SAU.matcher(norm).find()) {
            return today.plusMonths(1).withDayOfMonth(1);
        }
        if (P_NL_THANG_NAY.matcher(norm).find()) {
            return today.withDayOfMonth(1);
        }
        if (P_NL_QUY_NAY.matcher(norm).find()) {
            int q = (today.getMonthValue() - 1) / 3;
            return LocalDate.of(today.getYear(), q * 3 + 1, 1);
        }
        Matcher mThangX = P_NL_THANG_X.matcher(norm);
        if (mThangX.find()) {
            int month = Integer.parseInt(mThangX.group(1));
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("Thang khong hop le: " + month);
            }
            int year = P_NL_NAM_NAY.matcher(norm).find() || mThangX.group(2) != null
                    ? today.getYear()
                    : extractYear(norm, today.getYear());
            return LocalDate.of(year, month, 1);
        }
        if (P_NL_NAM_NAY.matcher(norm).find()) {
            // "nam nay" alone -> Jan 1 of current year
            return LocalDate.of(today.getYear(), 1, 1);
        }

        // 2. Pure ISO / Vietnamese formats
        if (P_YYYY_MM_DD.matcher(s).matches()) {
            try { return LocalDate.parse(s, ISO_DATE); }
            catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Ngay khong hop le: " + s, ex);
            }
        }
        if (P_YYYY_MM.matcher(s).matches()) {
            try {
                YearMonth ym = YearMonth.parse(padMonth(s), ISO_YEAR_MONTH);
                return ym.atDay(1);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Thang khong hop le: " + s, ex);
            }
        }
        if (P_DD_MM_YYYY.matcher(s).matches()) {
            try { return LocalDate.parse(s, VN_DDMMYYYY); }
            catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Ngay khong hop le: " + s, ex);
            }
        }
        if (P_MM_YYYY.matcher(s).matches()) {
            try {
                YearMonth ym = YearMonth.parse(s, VN_MMYYYY);
                return ym.atDay(1);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Thang khong hop le: " + s, ex);
            }
        }

        throw new IllegalArgumentException("Khong the parse ngay: '" + s + "'. Dinh dang ho tro: yyyy-MM-dd, yyyy-MM, dd/MM/yyyy, MM/yyyy hoac 'thang X nam nay'.");
    }

    public static LocalDate parseMonth(String raw) {
        return parse(raw);
    }

    private static String padMonth(String s) {
        int dash = s.indexOf('-');
        String year = s.substring(0, dash);
        String month = s.substring(dash + 1);
        if (month.length() == 1) month = "0" + month;
        return year + "-" + month;
    }

    private static int extractYear(String norm, int fallback) {
        Matcher m = Pattern.compile("(20\\d{2}|19\\d{2})").matcher(norm);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return fallback;
    }

    private static String stripAccents(String s) {
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n;
    }
}
