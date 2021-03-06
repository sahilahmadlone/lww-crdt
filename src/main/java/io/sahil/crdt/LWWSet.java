package io.sahil.crdt;

import io.sahil.crdt.helpers.Operations;

import java.util.Set;

public class LWWSet<E> {

    private GSet<ElementState<E>> addSet;

    private GSet<ElementState<E>> removeSet;

    public static class ElementState<E> {
        private long timestamp;
        private E element;

        public ElementState(long timestamp, E element) {
            this.timestamp = timestamp;
            this.element = element;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public E getElement() {
            return element;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ElementState<?> that = (ElementState<?>) o;

            return timestamp == that.timestamp && !(element != null ? !element.equals(that.element) : that.element != null);
        }

        @Override
        public int hashCode() {
            int result = (int) (timestamp ^ (timestamp >>> 32));
            result = 31 * result + (element != null ? element.hashCode() : 0);
            return result;
        }
    }

    public LWWSet() {
        this.addSet = new GSet<>();
        this.removeSet = new GSet<>();
    }

    LWWSet(GSet<ElementState<E>> addSet, GSet<ElementState<E>> removeSet) {
        this.addSet = addSet;
        this.removeSet = removeSet;
    }

    public void add(ElementState<E> elementState) {
        addSet.add(elementState);
    }

    public void remove(ElementState<E> elementState) {
        removeSet.add(elementState);
    }

    public LWWSet<E> merge(LWWSet<E> anotherLwwSet) {
        return new LWWSet<>(addSet.merge(anotherLwwSet.addSet), removeSet.merge(anotherLwwSet.removeSet));
    }

    public LWWSet<E> diff(LWWSet<E> anotherLwwSet) {
        final LWWSet<E> mergeResult = merge(anotherLwwSet);
        return new LWWSet<>(
                mergeResult.getAddSet().diff(anotherLwwSet.getAddSet()),
                mergeResult.getRemoveSet().diff(anotherLwwSet.getRemoveSet()));
    }

    public Set<E> lookup() {
        Set<ElementState<E>> lookup = addSet.lookup();
        return Operations.filteredAndMapped(lookup, new Operations.Predicate<ElementState<E>>() {
            @Override
            public boolean call(ElementState<E> element) {
                return nonRemoved(element);
            }
        }, new Operations.Mapper<ElementState<E>, E>() {
            @Override
            public E call(ElementState<E> element) {
                return element.element;
            }
        });
    }

    private boolean nonRemoved(final ElementState<E> addState) {
        Set<ElementState<E>> removes = Operations.filtered(removeSet.lookup(),
                new Operations.Predicate<ElementState<E>>() {
                    @Override
                    public boolean call(ElementState<E> element) {
                        return element.getElement().equals(addState.getElement())
                                && element.getTimestamp() > addState.getTimestamp();
                    }
                });
        return removes.isEmpty();
    }

    // Visible for testing
    GSet<ElementState<E>> getAddSet() {
        return new GSet<ElementState<E>>().merge(addSet);
    }

    // Visible for testing
    GSet<ElementState<E>> getRemoveSet() {
        return new GSet<ElementState<E>>().merge(removeSet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LWWSet<?> lwwSet = (LWWSet<?>) o;

        return addSet.equals(lwwSet.addSet) && removeSet.equals(lwwSet.removeSet);
    }

    @Override
    public int hashCode() {
        int result = addSet.hashCode();
        result = 31 * result + removeSet.hashCode();
        return result;
    }
}
