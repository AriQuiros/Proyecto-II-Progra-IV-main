package org.example.backendp2p4.util;

import org.example.backendp2p4.config.ParallelConfig;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

public class ParallelUtils {
    /*
     * Método genérico para transformar una lista usando paralelismo.
     *
     * T representa el tipo de dato original de la lista.
     * R representa el tipo de dato que se va a devolver después de aplicar el mapper.
     *
     * Ejemplo:
     * Si se recibe una lista de Medico, se puede devolver una lista de MedicoCardDTO.
     */
    public static <T, R> List<R> mapearEnParalelo(List<T> lista, Function<T, R> mapper) {

        /*
         * Se crea un ExecutorService usando la configuración del proyecto.

         */
        ExecutorService executor = ParallelConfig.crearExecutor();

        try {
            /*
             * Por cada elemento de la lista se crea una tarea independiente.
             *
             * Cada tarea ejecuta:
             * mapper.apply(item)
             *
             * Eso significa que cada elemento se transforma de manera separada.
             *
             * executor.submit(...) envía la tarea al pool de hilos.
             * Si hay varios hilos disponibles, varias tareas se ejecutan al mismo tiempo.
             *
             * Cada submit devuelve un Future<R>.
             * Un Future representa un resultado que todavía puede estar ejecutándose.
             */
            List<Future<R>> futuros = lista.stream()
                    .map(item -> executor.submit(() -> mapper.apply(item)))
                    .toList();

            /*
             * Después de lanzar todas las tareas en paralelo,
             * se recorren los Future para obtener los resultados.
             *
             * futuro.get() espera a que la tarea termine y devuelve su resultado.
             *
             * Aunque get() espera, las tareas ya fueron lanzadas previamente,
             * por lo que se están ejecutando en paralelo en el ExecutorService.
             */
            return futuros.stream()
                    .map(futuro -> {
                        try {
                            /*
                             * Obtiene el resultado de la tarea.
                             *
                             * Si la tarea aún no terminó, este punto espera.
                             */
                            return futuro.get();
                        } catch (Exception e) {
                            /*
                             * Si ocurre un error dentro de alguna tarea paralela,
                             * se lanza una excepción para indicar que falló el procesamiento.
                             */
                            throw new RuntimeException("Error ejecutando tarea en paralelo", e);
                        }
                    })
                    .toList();

        } finally {
            /*
             * Se apaga el ExecutorService después de terminar.
             *
             * Esto libera los hilos creados y evita que queden ejecutándose
             * innecesariamente en memoria.
             *
             * El bloque finally asegura que el executor se cierre incluso
             * si ocurre una excepción durante el procesamiento.
             */
            executor.shutdown();
        }
    }
}