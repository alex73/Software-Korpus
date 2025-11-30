package org.alex73.korpus.base;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

public class tt {

    public static void main(String[] args) {
        int[] v = new int[] { 1, 2, 3, 4 };
        StreamSupport.stream(new Spliterator<Object>() {
            int po = -1;

            @Override
            public boolean tryAdvance(Consumer<? super Object> action) {
                po++;
                if (po < v.length) {
                    action.accept(v[po]);
                    return true;
                }
                return false;
            }

            @Override
            public Spliterator<Object> trySplit() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public long estimateSize() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public int characteristics() {
                return 0;
            }
        }, false).forEach(System.out::println);
    }
}
