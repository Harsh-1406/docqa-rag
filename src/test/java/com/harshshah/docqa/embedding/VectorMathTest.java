package com.harshshah.docqa.embedding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VectorMathTest {

    @Test
    void normalizedVectorHasUnitLength() {
        float[] n = VectorMath.normalize(new float[]{3f, 4f});
        assertThat(n[0]).isCloseTo(0.6f, within(1e-6f));
        assertThat(n[1]).isCloseTo(0.8f, within(1e-6f));
    }

    @Test
    void zeroVectorIsReturnedUnchanged() {
        assertThat(VectorMath.normalize(new float[]{0f, 0f})).containsExactly(0f, 0f);
    }
}