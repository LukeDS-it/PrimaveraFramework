package it.ldsoftware.primavera.vaadin.data;

import com.vaadin.data.Container.Filterable;
import com.vaadin.data.Container.Sortable;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.data.util.filter.UnsupportedFilterException;
import it.ldsoftware.primavera.query.factories.FilterProcessor;
import org.vaadin.viritin.ListContainer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static it.ldsoftware.primavera.query.FilterOperator.AND;
import static java.util.Collections.unmodifiableCollection;

/**
 * Created by luca on 19/04/16.
 * Container for filterable lazy lists
 */
public class FilterableLazyListContainer<T> extends ListContainer<T> implements Filterable, Sortable {

    private static final long serialVersionUID = 1L;

    private final Set<Filter> filters = new HashSet<>();
    private final Set<FilterProcessor> processors = new HashSet<>();

    public FilterableLazyListContainer(FilterableLazyList<T> backingList) {
        super(backingList);
    }

    public FilterableLazyListContainer(Class<T> type, FilterableLazyList<T> backingList) {
        super(type, backingList);
    }


    @Override
    public void addContainerFilter(Filter filter) throws UnsupportedFilterException {
        if (filters.add(filter)) {
            ((FilterableLazyList<T>) getBackingList()).addFilter(convertFilter(filter));
            applyFilters();
        }
    }

    @Override
    public void removeContainerFilter(Filter filter) {
        if (filters.remove(filter)) {
            ((FilterableLazyList<T>) getBackingList()).removeFilter(convertFilter(filter));
            applyFilters();
        }
    }

    @Override
    public void removeAllContainerFilters() {
        if (!filters.isEmpty()) {
            filters.clear();
            ((FilterableLazyList<T>) getBackingList()).removeAllFilters();
            applyFilters();
        }
    }

    @Override
    public Collection<Filter> getContainerFilters() {
        return unmodifiableCollection(filters);
    }

    public void refresh() {
        applyFilters();
    }

    public void addCustomFilterProcessor(FilterProcessor processor) {
        processors.add(processor);
    }

    private void applyFilters() {
        ((FilterableLazyList<T>) getBackingList()).reset();
        super.fireItemSetChange();
    }

    private it.ldsoftware.primavera.query.Filter convertFilter(Filter filter) {
        if (!(filter instanceof SimpleStringFilter))
            throw new UnsupportedFilterException("Only SimpleStringFilter is supported");
        return convertFilter((SimpleStringFilter) filter);
    }

    private it.ldsoftware.primavera.query.Filter convertFilter(SimpleStringFilter filter) {
        it.ldsoftware.primavera.query.Filter f;
        for (FilterProcessor processor: processors) {
            f = processor.createFilterFor(filter.getPropertyId().toString(), filter.getFilterString() + "%");
            if (f != null)
                return f;
        }
        return new it.ldsoftware.primavera.query.Filter(filter.getPropertyId().toString(), filter.getFilterString() + "%", false, AND);
    }

}