package com.harshshah.docqa.embedding;

final class VectorMath {

    private VectorMath() {}

    /** L2-normalises. Needed because Gemini only returns unit-length vectors at full size, not when truncated to 768. */
    static float[] normalize(float[] v) {
        double sum = 0;
        for (float x : v) sum += (double) x * x;
        double norm = Math.sqrt(sum);
        if (norm == 0) return v;
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (float) (v[i] / norm);
        return out;
    }
}