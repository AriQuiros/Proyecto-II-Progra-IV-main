package org.example.backendp2p4.util;

import org.example.backendp2p4.config.ParallelConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

public class ParallelUtils {

    /*
     * Método genérico para transformar una lista usando paralelismo.
     *
     * T representa el tipo de dato original de la lista.
     * R representa el tipo de dato que se devuelve después de aplicar el mapper.
     *
     * Ejemplo:
     * Si se recibe una lista de Medico, se puede devolver una lista de MedicoCardDTO.
     *
     * Este método usa el ExecutorService definido en ParallelConfig.
     * La cantidad de hilos se controla desde:
     *
     * ParallelConfig.CANTIDAD_HILOS
     */
    public static <T, R> List<R> mapearEnParalelo(List<T> lista, Function<T, R> mapper) {

        /*
         * Se crea un ExecutorService usando la configuración del proyecto.
         *
         * Internamente, ParallelConfig.crearExecutor() crea un pool de hilos fijo.
         * Esto permite probar el rendimiento cambiando manualmente la cantidad de hilos.
         */
        ExecutorService executor = ParallelConfig.crearExecutor();

        try {
            List<Future<R>> futuros = new ArrayList<>();

            /*
             * Por cada elemento de la lista se crea una tarea independiente.
             *
             * Cada tarea ejecuta:
             *
             * mapper.apply(item)
             *
             * Esto permite que varios elementos se transformen al mismo tiempo,
             * dependiendo de la cantidad de hilos configurada.
             */
            for (T item : lista) {
                Future<R> futuro = executor.submit(() -> mapper.apply(item));
                futuros.add(futuro);
            }

            List<R> resultado = new ArrayList<>();

            /*
             * Se obtienen los resultados de las tareas paralelas.
             *
             * futuro.get() espera a que la tarea termine.
             * Aunque aquí se recopilan los resultados uno por uno,
             * las tareas ya fueron enviadas previamente al ExecutorService,
             * por lo que se ejecutan en paralelo.
             */
            for (Future<R> futuro : futuros) {
                try {
                    resultado.add(futuro.get());
                } catch (InterruptedException e) {
                    /*
                     * Si el hilo actual es interrumpido, se restaura el estado
                     * de interrupción y se lanza una excepción controlada.
                     */
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("El mapeo paralelo fue interrumpido.", e);
                } catch (ExecutionException e) {
                    /*
                     * Si una tarea paralela falla, se captura el error y se informa.
                     */
                    throw new RuntimeException("Error ejecutando tarea de mapeo en paralelo.", e);
                }
            }

            return resultado;

        } finally {
            /*
             * Se apaga el ExecutorService para liberar los hilos creados.
             *
             * El bloque finally asegura que el executor se cierre incluso si ocurre
             * una excepción durante el procesamiento.
             */
            executor.shutdown();
        }
    }

    /*
     * Método genérico para ordenar una lista en paralelo.
     *
     * T representa el tipo de dato de la lista.
     *
     * Este método recibe:
     * - lista: colección que se desea ordenar.
     * - comparator: criterio de comparación usado para ordenar.
     *
     * En el proyecto se usa para ordenar médicos por costo de consulta,
     * de menor a mayor.
     *
     * A diferencia de parallelStream(), este método sí usa el ExecutorService
     * configurado en ParallelConfig, por lo que se puede controlar manualmente
     * la cantidad de hilos mediante:
     *
     * ParallelConfig.CANTIDAD_HILOS
     */
    public static <T> List<T> ordenarEnParalelo(List<T> lista, Comparator<T> comparator) {

        if (lista == null || lista.size() <= 1) {
            return lista;
        }

        /*
         * Se crea el pool de hilos usando la configuración del proyecto.
         */
        ExecutorService executor = ParallelConfig.crearExecutor();

        try {
            /*
             * Math.max(1, ...) evita que se use una cantidad inválida de hilos.
             */
            int cantidadHilos = Math.max(1, ParallelConfig.CANTIDAD_HILOS);

            /*
             * No tiene sentido crear más bloques que elementos.
             *
             * Ejemplo:
             * Si hay 3 médicos y 5 hilos, solo se crean 3 bloques.
             */
            int cantidadBloques = Math.min(cantidadHilos, lista.size());

            /*
             * Se calcula el tamaño aproximado de cada bloque.
             *
             * Ejemplo:
             * Si hay 100 elementos y 5 hilos, cada bloque tendrá cerca de 20 elementos.
             */
            int tamanoBloque = (int) Math.ceil((double) lista.size() / cantidadBloques);

            List<Future<List<T>>> futuros = new ArrayList<>();

            /*
             * La lista se divide en bloques.
             *
             * Cada bloque se copia en una nueva lista para evitar modificar
             * directamente la lista original mientras los hilos trabajan.
             */
            for (int i = 0; i < lista.size(); i += tamanoBloque) {
                int inicio = i;
                int fin = Math.min(i + tamanoBloque, lista.size());

                List<T> bloque = new ArrayList<>(lista.subList(inicio, fin));

                /*
                 * Cada bloque se ordena en una tarea independiente.
                 *
                 * Si hay varios hilos disponibles, varios bloques pueden ordenarse
                 * al mismo tiempo.
                 */
                Future<List<T>> futuro = executor.submit(() -> {
                    bloque.sort(comparator);
                    return bloque;
                });

                futuros.add(futuro);
            }

            List<List<T>> bloquesOrdenados = new ArrayList<>();

            /*
             * Se recuperan los bloques ya ordenados.
             */
            for (Future<List<T>> futuro : futuros) {
                try {
                    bloquesOrdenados.add(futuro.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("El ordenamiento paralelo fue interrumpido.", e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("Error ejecutando tarea de ordenamiento en paralelo.", e);
                }
            }

            /*
             * Una vez que cada bloque está ordenado, se mezclan los bloques
             * para construir una única lista final ordenada.
             */
            return mezclarBloquesOrdenados(bloquesOrdenados, comparator);

        } finally {
            /*
             * Se apaga el ExecutorService para liberar recursos.
             */
            executor.shutdown();
        }
    }

    /*
     * Mezcla varios bloques previamente ordenados en una sola lista ordenada.
     *
     * Este método funciona como la fase final de un merge sort:
     *
     * 1. Cada bloque ya viene ordenado.
     * 2. Se compara el primer elemento disponible de cada bloque.
     * 3. Se toma el menor según el comparator.
     * 4. Se avanza dentro del bloque correspondiente.
     *
     * Esto evita simplemente pegar los bloques y ordenar todo otra vez desde cero.
     */
    private static <T> List<T> mezclarBloquesOrdenados(
            List<List<T>> bloques,
            Comparator<T> comparator
    ) {
        List<T> resultado = new ArrayList<>();

        /*
         * PriorityQueue permite obtener siempre el menor elemento disponible
         * entre todos los bloques ordenados.
         */
        PriorityQueue<NodoOrdenamiento<T>> cola = new PriorityQueue<>(
                (a, b) -> comparator.compare(a.valor, b.valor)
        );

        /*
         * Se agrega a la cola el primer elemento de cada bloque.
         */
        for (int i = 0; i < bloques.size(); i++) {
            List<T> bloque = bloques.get(i);

            if (!bloque.isEmpty()) {
                cola.add(new NodoOrdenamiento<>(bloque.get(0), i, 0));
            }
        }

        /*
         * Mientras existan elementos pendientes, se extrae el menor elemento
         * y se agrega a la lista resultado.
         */
        while (!cola.isEmpty()) {
            NodoOrdenamiento<T> nodo = cola.poll();

            resultado.add(nodo.valor);

            int siguienteIndice = nodo.indiceElemento + 1;
            List<T> bloque = bloques.get(nodo.indiceBloque);

            /*
             * Si el bloque del cual se tomó el elemento todavía tiene más datos,
             * se agrega el siguiente elemento de ese mismo bloque a la cola.
             */
            if (siguienteIndice < bloque.size()) {
                cola.add(new NodoOrdenamiento<>(
                        bloque.get(siguienteIndice),
                        nodo.indiceBloque,
                        siguienteIndice
                ));
            }
        }

        return resultado;
    }

    /*
     * Clase auxiliar usada durante la mezcla de bloques ordenados.
     *
     * Guarda:
     * - valor: elemento actual.
     * - indiceBloque: bloque al que pertenece el elemento.
     * - indiceElemento: posición del elemento dentro de su bloque.
     */
    private static class NodoOrdenamiento<T> {
        private final T valor;
        private final int indiceBloque;
        private final int indiceElemento;

        public NodoOrdenamiento(T valor, int indiceBloque, int indiceElemento) {
            this.valor = valor;
            this.indiceBloque = indiceBloque;
            this.indiceElemento = indiceElemento;
        }
    }
}