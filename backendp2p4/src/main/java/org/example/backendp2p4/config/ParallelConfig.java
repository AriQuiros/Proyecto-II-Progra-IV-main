package org.example.backendp2p4.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelConfig {

    // Cambia este número cuando quieras probar con más o menos hilos
    public static int CANTIDAD_HILOS = 4;

    public static ExecutorService crearExecutor() {
        int hilos = Math.max(1, CANTIDAD_HILOS);
        return Executors.newFixedThreadPool(hilos);
    }
}