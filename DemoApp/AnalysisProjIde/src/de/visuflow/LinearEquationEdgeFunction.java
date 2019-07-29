package de.visuflow;

import heros.EdgeFunction;
import heros.edgefunc.EdgeIdentity;

import java.math.BigDecimal;
import java.nio.channels.OverlappingFileLockException;

import static de.visuflow.InterProceduralAnalysis.BOTTOM_ELEMENT;
import static de.visuflow.InterProceduralAnalysis.TOP_ELEMENT;

// Represents linear equations of the form 'y = a*x + b'
public class LinearEquationEdgeFunction implements EdgeFunction<Integer> {

    // int is not sufficient here as it can't represent division
    // (e.g. "y = x / 4")
    public final BigDecimal a;

    public final int b;

    public LinearEquationEdgeFunction(int a, int b) {
        this(new BigDecimal(a), b);
    }

    public LinearEquationEdgeFunction(BigDecimal a, int b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public Integer computeTarget(Integer source) {
        if (a.equals(BigDecimal.ZERO))
            return b;
        else if (source == TOP_ELEMENT)
            return TOP_ELEMENT;
        else if (source == BOTTOM_ELEMENT)
            return b; // 'a * BOTTOM + b' is probably impossible

        return a.multiply(new BigDecimal(source)).intValue() + b;
    }

    @Override
    public EdgeFunction<Integer> composeWith(EdgeFunction<Integer> secondFunction) {
        if (secondFunction instanceof LinearEquationEdgeFunction) {
            // f(x) = a1 x + b1
            // g(x) = a2 (a1 x + b1) + b2
            // f(g(x)) = a2 a1 x + a2 b1 + b2
            LinearEquationEdgeFunction other = (LinearEquationEdgeFunction) secondFunction;
            return new LinearEquationEdgeFunction(
                    other.a.multiply(a).stripTrailingZeros(),
                    other.a.multiply(new BigDecimal(b)).intValue() + other.b);
        } else if (secondFunction instanceof TopEdgeFunction) {
            return secondFunction;
        } else if (secondFunction instanceof EdgeIdentity) {
            return this;
        }
        throw new OverlappingFileLockException();
    }

    @Override
    public EdgeFunction<Integer> joinWith(EdgeFunction<Integer> otherFunction) {
        if (equalTo(otherFunction)) {
            return this;
        }

        return new TopEdgeFunction();
    }

    @Override
    public boolean equalTo(EdgeFunction<Integer> other) {
        if (other instanceof LinearEquationEdgeFunction) {
            LinearEquationEdgeFunction other2 = (LinearEquationEdgeFunction) other;
            return a.equals(other2.a) && b == other2.b;
        }
        return false;
    }
}
