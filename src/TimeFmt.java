import java.math.*;


public class TimeFmt {
    public static final String[] IN_UNITS = {"Seconds", "Minutes", "Hours"};
    public static final String[] OUT_UNITS = {"Clock time (h:mm:ss)", "Seconds", "Minutes", "Hours"};
    public static final String[] ABBR = {"s", "min", "h"};                          // matches IN_UNITS
    private static final BigDecimal[] SECONDS = {BigDecimal.ONE, new BigDecimal("60"), new BigDecimal("3600")};

    private final int scale, decimals;
    private final BigDecimal factor;
    private final boolean clock;

    public TimeFmt(int scale, int inIdx, int outIdx, int decimals) {
        this.scale = scale; this.decimals = decimals;
        this.clock = outIdx == 0;
        this.factor = clock ? SECONDS[inIdx] : SECONDS[inIdx].divide(SECONDS[outIdx - 1], MathContext.DECIMAL128);
    }

    public static String outLabel(int outIdx) { return outIdx == 0 ? "h:mm:ss" : ABBR[outIdx - 1]; }

    public String format(long t) {
        BigDecimal v = BigDecimal.valueOf(t, scale).multiply(factor);
        return clock ? clock(v, -1) : v.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }

    public String average(BigInteger sum, int n) {
        BigDecimal v = new BigDecimal(sum, scale).multiply(factor).divide(BigDecimal.valueOf(n), MathContext.DECIMAL128);
        return clock ? clock(v, decimals) : v.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }

    public String averageInput(BigInteger sum, int n) {
        BigDecimal v = new BigDecimal(sum, scale).divide(BigDecimal.valueOf(n), MathContext.DECIMAL128);
        return v.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }

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
