package info.kgeorgiy.ja.osipov.arrayset;

import java.util.*;

public class ReversibleList<E> extends AbstractList<E> implements RandomAccess {

    final List<E> data;
    boolean reversed;

    public ReversibleList(final List<E> list, final boolean reversed) {
        this.data = list;
        this.reversed = reversed;
    }

    public ReversibleList(final Collection<? extends E> collection, final boolean reversed) {
        this.data = List.copyOf(collection);
        this.reversed = reversed;
    }

    @Override
    public E get(final int index) {
        return data.get(reversed ? size() - index - 1 : index);
    }

    @Override
    public int size() {
        return data.size();
    }
}
