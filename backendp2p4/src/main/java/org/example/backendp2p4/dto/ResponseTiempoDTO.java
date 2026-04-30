package org.example.backendp2p4.dto;

public class ResponseTiempoDTO<T> {
    private T data;
    private long tiempo;

    public ResponseTiempoDTO(T data, long tiempo) {
        this.data = data;
        this.tiempo = tiempo;
    }

    public T getData() { return data; }
    public long getTiempo() { return tiempo; }
}