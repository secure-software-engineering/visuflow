package de.visuflow;

import heros.EdgeFunction;

import static de.visuflow.InterProceduralAnalysis.TOP_ELEMENT;

public class TopEdgeFunction implements EdgeFunction<Integer> {

    @Override
    public Integer computeTarget(Integer source) {
        return TOP_ELEMENT;
    }

    @Override
    public EdgeFunction<Integer> composeWith(EdgeFunction<Integer> secondFunction) {
        return this;
    }

    @Override
    public EdgeFunction<Integer> joinWith(EdgeFunction<Integer> otherFunction) {
        return this;
    }

    @Override
    public boolean equalTo(EdgeFunction<Integer> other) {
        return getClass() == other.getClass();
    }
}
