package info.kgeorgiy.ja.Maksonov.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {

    private final List<E> elements;
    private final Comparator<? super E> comparator;

    //==================================================================//

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(E[] elements) {
        this(Arrays.asList(elements), null);
    }

    public ArraySet(Collection<? extends E> elements) {
        this(elements, null);
    }

    public ArraySet(Collection<? extends E> elements, Comparator<? super E> comparator) {
        this.comparator = comparator;
        this.elements = makeSet(new ArrayList<>(elements));
    }

    //==================================================================//
    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public E first() {
        checkSize();
        return elements.get(0);
    }

    @Override
    public E last() {
        checkSize();
        return elements.get(elements.size() - 1);
    }

    @Override
    public boolean contains(Object object) {
        return Collections.binarySearch(elements, (E) object, comparator) >= 0;
    }

    @Override
    public Iterator<E> iterator() {
        return elements.iterator();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> headSet(E toElement) throws IllegalArgumentException {
        int to = search(toElement);
        List<E> newElements = elements.subList(0, to);
        return new ArraySet<> (newElements, comparator);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        int from = search(fromElement);
        List<E> newElements = elements.subList(from, elements.size());
        return new ArraySet<> (newElements, comparator);
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        checkBounds(fromElement, toElement);

        int from = search(fromElement);
        int to = search(toElement);
        List<E> newElements = elements.subList(from, to);
        return new ArraySet<> (newElements, comparator);
    }

    //==================================================================//

    private List<E> makeSet(List<E> elements) {
        if (elements.isEmpty()) {
            return elements;
        }

        List<E> sortedAndUniqueElements = new ArrayList<>();
        elements.sort(comparator);
        E prev = elements.get(0);
        sortedAndUniqueElements.add(prev);
        for (int i = 1; i < elements.size(); i++) {
            E next = elements.get(i);
            if (!(comparator == null && prev.equals(next) || comparator != null && comparator.compare(prev, next) == 0)) {
                sortedAndUniqueElements.add(next);
            }
            prev = next;
        }

        return sortedAndUniqueElements;
    }

    public int search(E element) {
        int result = Collections.binarySearch(elements, element, comparator);
        return result >= 0 ? result : -(result + 1);
    }

    private void checkSize() {
        if (elements.size() <= 0) {
            throw new NoSuchElementException();
        }
    }

    private void checkBounds(E from, E to) throws IllegalArgumentException {
        if (comparator != null && comparator.compare(from, to) >= 0) {
            throw new IllegalArgumentException();
        }
    }
}
