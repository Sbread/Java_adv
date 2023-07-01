package info.kgeorgiy.ja.osipov.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {

    private final ReversibleList<E> data;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(final Comparator<? super E> comparator) {
        this(Collections.emptyList(), comparator);
    }

    private ArraySet(final List<E> data, final Comparator<? super E> comparator, final boolean reversed) {
        this.data =
                data instanceof ReversibleList<E> list
                ? new ReversibleList<>(list.data, reversed)
                : new ReversibleList<>(data, reversed);
        this.comparator = comparator;
    }

    public ArraySet(final Collection<? extends E> collection) {
        this(collection, null);
    }

    @SuppressWarnings("unchecked")
    public ArraySet(final Collection<? extends E> collection, final Comparator<? super E> comparator) {
        final Set<E> set = new TreeSet<>(comparator);
        set.addAll(collection);
        this.data = new ReversibleList<>(set, false);
        this.comparator = Objects.isNull(comparator) ? (Comparator<? super E>) Comparator.naturalOrder() : comparator;
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator.equals(Comparator.naturalOrder()) ? null : comparator;
    }

    private E getElementByIndexOrNull(final int index) {
        return 0 <= index && index < size() ? data.get(index) : null;
    }

    private int getBoundaryIndex(final E e, final int shift, final boolean withBound) {
        int index = Collections.binarySearch(data, e, comparator);
        if (index >= 0) {
            return withBound ? index : index + shift;
        }
        return shift > 0 ? -index - 1 : -index - 2;
    }

    @Override
    public E lower(final E o) {
        return getElementByIndexOrNull(getBoundaryIndex(o, -1, false));
    }

    @Override
    public E floor(final E o) {
        return getElementByIndexOrNull(getBoundaryIndex(o, -1, true));
    }

    @Override
    public E ceiling(final E o) {
        return getElementByIndexOrNull(getBoundaryIndex(o, 1, true));
    }

    @Override
    public E higher(final E o) {
        return getElementByIndexOrNull(getBoundaryIndex(o, 1, false));
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final Object o) {
        return Collections.binarySearch(data, (E) o, comparator) >= 0;
    }

    @Override
    public Iterator<E> iterator() {
        return data.iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(data, Collections.reverseOrder(comparator), !data.reversed);
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    private int compareElements(final E a, final E b) {
        return comparator.compare(a, b);
    }

    @Override
    public NavigableSet<E> subSet(final E fromElement, final boolean fromInclusive,
                                  final E toElement, final boolean toInclusive) {
        if (compareElements(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }
        int leftIndex = getBoundaryIndex(fromElement, 1, fromInclusive);
        int rightIndex = getBoundaryIndex(toElement, -1, toInclusive);
        return leftIndex > rightIndex ? new ArraySet<E>(comparator)
                : new ArraySet<>(data.subList(leftIndex, rightIndex + 1), comparator, data.reversed);
    }

    @Override
    public NavigableSet<E> headSet(final E toElement, final boolean inclusive) {
        if (data.isEmpty()) {
            return this;
        }
        E fromElement = compareElements(first(), toElement) < 0 ? first() : toElement;
        return subSet(fromElement, true, toElement, inclusive);
    }

    @Override
    public NavigableSet<E> tailSet(final E fromElement, final boolean inclusive) {
        if (data.isEmpty()) {
            return this;
        }
        E toElement = compareElements(last(), fromElement) > 0 ? last() : fromElement;
        return subSet(fromElement, inclusive, toElement, true);
    }

    @Override
    public SortedSet<E> subSet(final E fromElement, final E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(final E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(final E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E first() {
        if (data.isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(0);
    }

    @Override
    public E last() {
        if (data.isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(size() - 1);
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }
}
