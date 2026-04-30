package org.example.backendp2p4.util;

import org.example.backendp2p4.config.ParallelConfig;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

public class ParallelUtils {

    public static <T, R> List<R> mapearEnParalelo(List<T> lista, Function<T, R> mapper) {
        ExecutorService executor = ParallelConfig.crearExecutor();

        try {
            List<Future<R>> futuros = lista.stream()
                    .map(item -> executor.submit(() -> mapper.apply(item)))
                    .toList();

            return futuros.stream()
                    .map(futuro -> {
                        try {
                            return futuro.get();
                        } catch (Exception e) {
                            throw new RuntimeException("Error ejecutando tarea en paralelo", e);
                        }
                    })
                    .toList();

        } finally {
            executor.shutdown();
        }
    }
}