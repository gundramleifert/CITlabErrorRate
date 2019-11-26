package de.uros.citlab.errorrate.kws;

public class QueryConfig {
    public static class Builder {
        QueryConfig c = new QueryConfig();

        public Builder(boolean upper, boolean part) {
            c.upper = upper;
            c.part = part;
        }

        public Builder setLength(int min, int maxIncl) {
            c.minLen = min;
            c.maxLen = maxIncl;
            return this;
        }

        public Builder setOccurance(int min, int maxIncl) {
            c.minOcc = min;
            c.maxOcc = maxIncl;
            return this;
        }

        public Builder setAlpha(boolean onlyAlpha) {
            c.alpha = onlyAlpha;
            return this;
        }

        public QueryConfig build() {
            return c;
        }
    }

    public int getMinLen() {
        return minLen;
    }

    public int getMaxLen() {
        return maxLen;
    }

    public int getMinOcc() {
        return minOcc;
    }

    public int getMaxOcc() {
        return maxOcc;
    }

    public boolean isAlpha() {
        return alpha;
    }

    public boolean isUpper() {
        return upper;
    }

    public boolean isPart() {
        return part;
    }

    @Override
    public String toString() {
        return "QueryConfig{" +
                "minLen=" + minLen +
                ", maxLen=" + maxLen +
                ", minOcc=" + minOcc +
                ", maxOcc=" + maxOcc +
                ", alpha=" + alpha +
                ", upper=" + upper +
                ", part=" + part +
                '}';
    }

    private int minLen = 0;
    private int maxLen = -1;
    private int minOcc = 0;
    private int maxOcc = -1;
    private boolean alpha = false;
    private boolean upper = false;
    private boolean part = false;
}
