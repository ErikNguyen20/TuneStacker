package com.example.cloudplaylistmanager.Utils;

import java.io.Serializable;

public class SerializablePair<F, S> implements Serializable {
    public F first;
    public S second;

    public SerializablePair() {

    }

    public SerializablePair(F first, S second) {
        this.first = first;
        this.second = second;
    }
}
