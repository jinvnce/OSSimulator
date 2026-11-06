import java.math.*;

/**
 * Turns the exact internal times (whole numbers in fixed point) into text for the screen.
 *  - scale    : how many decimal places the internal numbers carry (25 with scale 1 means 2.5)
 *  - decimals : decimal places shown in the averages and in converted numbers
 * The person picks the unit the times are typed in (seconds / minutes / hours) and what to show them as:
 *  - clock time H:MM:SS (3725 s -> 1:02:05, a fraction of a second only when there is one), or
 *  - another unit (seconds / minutes / hours) with the chosen decimal places, e.g. 90 s -> 1.50 min.
 */
public class TimeFmt {
    public static final String[] IN_UNITS = {"Seconds", "Minutes", "Hours"};
    public static final String[] OUT_UNITS = {"Clock time (h:mm:ss)", "Seconds", "Minutes", "Hours"};
    public static final String[] ABBR = {"s", "min", "h"};                          // matches IN_UNITS
    private static final BigDecimal[] SECONDS = {BigDecimal.ONE, new BigDecimal("60"), new BigDecimal("3600")};

    private final int scale, decimals;
    private final BigDecimal factor;      // clock: seconds per input unit;  numbers: output units per input unit
    private final boolean clock;

    /** inIdx: index in IN_UNITS. outIdx: index in OUT_UNITS (0 = clock time, 1..3 = seconds / minutes / hours). */
    public TimeFmt(int scale, int inIdx, int outIdx, int decimals) {
        this.scale = scale; this.decimals = decimals;
        this.clock = outIdx == 0;
        this.factor = clock ? SECONDS[inIdx] : SECONDS[inIdx].divide(SECONDS[outIdx - 1], MathContext.DECIMAL128);
    }

    /** Label for result column titles, e.g. "h:mm:ss" or "min". */
    public static String outLabel(int outIdx) { return outIdx == 0 ? "h:mm:ss" : ABBR[outIdx - 1]; }

    public String format(long t) {
        BigDecimal v = BigDecimal.valueOf(t, scale).multiply(factor);
        return clock ? clock(v, -1) : v.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }

    /** Average of 'sum' (fixed point) over n values, with the chosen decimals. */
    public String average(BigInteger sum, int n) {
        BigDecimal v = new BigDecimal(sum, scale).multiply(factor).divide(BigDecimal.valueOf(n), MathContext.DECIMAL128);
        return clock ? clock(v, decimals) : v.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }

    /** The same average as a plain number in the input unit, for "= ... in the original unit". */
    public String averageInput(BigInteger sum, int n) {
        BigDecimal v = new BigDecimal(sum, scale).divide(BigDecimal.valueOf(n), MathContext.DECIMAL128);
        return v.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }

    /** Seconds -> H:MM:SS. fixed < 0 means "exact, no padding zeros"; otherwise exactly 'fixed' decimals on the seconds. */
    private String clock(BigDecimal seconds, int fixed) {
        BigDecimal r = fixed >= 0 ? seconds.setScale(fixed, RoundingMode.HALF_UP) : seconds.stripTrailingZeros();
        if (r.scale() < 0) r = r.setScale(0);
        BigInteger whole = r.toBigInteger();
        BigDecimal frac = r.subtract(new BigDecimal(whole));
        BigInteger[] hm = whole.divideAndRemainder(BigInteger.valueOf(3600));
        BigInteger[] ms = hm[1].divideAndRemainder(BigInteger.valueOf(60));
        String fracText = r.scale() > 0 ? frac.toPlainString().substring(1) : "";   // ".5"
        return hm[0] + ":" + pad(ms[0]) + ":" + pad(ms[1]) + fracText;
    }

    private static String pad(BigInteger v) { return v.intValue() < 10 ? "0" + v : v.toString(); }
}
