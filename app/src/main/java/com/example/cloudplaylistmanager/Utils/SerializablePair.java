package com.example.cloudplaylistmanager.Utils;

import java.io.Serializable;

/**
 * Simple pair class that implements {@link Serializable}.
 * @param <F> Generic type for the first value.
 * @param <S> Generic type for the second value.
 */
public class SerializablePair<F, S> implements Serializable {
    public F first;
    public S second;

    /**
     * Instantiates a new Serializable Pair Object.
     * @param first First object value.
     * @param second Second object value.
     */
    public SerializablePair(F first, S second) {
        this.first = first;
        this.second = second;
    }
}
