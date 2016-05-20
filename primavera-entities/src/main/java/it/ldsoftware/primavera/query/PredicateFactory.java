package it.ldsoftware.primavera.query;


import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.path.CollectionPath;
import com.mysema.query.types.path.PathBuilder;
import com.mysema.query.types.path.StringPath;
import it.ldsoftware.primavera.entities.base.BaseEntity;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static it.ldsoftware.primavera.query.FilterOperator.AND;
import static it.ldsoftware.primavera.util.CalendarUtil.endOfDay;
import static it.ldsoftware.primavera.util.CalendarUtil.fromDate;
import static it.ldsoftware.primavera.util.ReflectionUtil.getPropertyFromMethod;

/**
 * Created by luca on 11/04/16.
 * This class serves as a dynamic QueryDSL predicate factory.
 * Using reflection and dynamic paths, this class can easily create predicates based on a filter list
 * or on an example entity.
 */
public class PredicateFactory {
    private static final Logger logger = Logger.getLogger(PredicateFactory.class);

    /**
     * This function returns the boolean expression that corresponds to given entity and filters.
     *
     * @param eClass  entity class used to build the dynamic path
     * @param filters collection of filters
     * @param <E>     parameter for type-safe handling
     * @return the predicate in the form of a BooleanExpression
     */
    public static <E extends BaseEntity> BooleanExpression createPredicate(Class<E> eClass, Collection<Filter> filters) {
        String entityName = eClass.getSimpleName();
        entityName = entityName.substring(0, 1).toLowerCase() + entityName.substring(1);
        if (entityName.equals("group"))
            entityName += "1";
        PathBuilder<E> pb = new PathBuilder<>(eClass, entityName);
        BooleanExpression partial = pb.isNotNull();

        Set<String> betweenParsed = new HashSet<>();

        for (Filter filter : filters) {
            try {
                Field f = eClass.getDeclaredField(filter.getProperty());

                if (Collection.class.isAssignableFrom(f.getType())) {
                    partial = handleCollection(pb, partial, filter);
                } else if (String.class.isAssignableFrom(f.getType())) {
                    partial = handleString(pb, partial, filter);
                } else if (BaseEntity.class.isAssignableFrom(f.getType())) {
                    partial = handleEntity(pb, partial, filter, f);
                } else {
                    partial = handleOther(pb, partial, filter);
                }
            } catch (NoSuchFieldException ignored) {
                String fname = filter.getProperty();
                if (fname.toLowerCase().endsWith("from") || fname.toLowerCase().endsWith("to")) {
                    if (!betweenParsed.contains(getBetweenFieldName(fname)))
                        partial = handleBetween(pb, partial, filter, filters);
                    betweenParsed.add(getBetweenFieldName(fname));

                } else if (fname.contains(".") && !fname.endsWith(".")) {
                    // TODO handle subproperties
                    // String[] subProperties = fname.split("\\.");
                } else {
                    logger.debug("The field " + filter.getProperty()
                            + " does not exist in the entity. Remember to add any custom query after the call.");
                }
            }
        }

        return partial;
    }

    public static <E extends BaseEntity> BooleanExpression createPredicate(Class<E> entityClass, E entity) {
        return createPredicate(entityClass, getFiltersByEntity(entityClass, entity));
    }

    public static <E extends BaseEntity> List<Filter> getFiltersByEntity(Class<E> entityClass, E entity) {
        List<Filter> filters = new ArrayList<>();
        Arrays.stream(entityClass.getDeclaredMethods()).filter(PredicateFactory::isCandidate)
                .map(m -> filterFromMethod(m, entity)).filter(f -> f != null).forEach(filters::add);
        return filters;
    }

    private static boolean isCandidate(Method method) {
        return (method.getName().startsWith("get") || method.getName().startsWith("is"))
                && !(method.getName().equals("getId") || method.getName().equals("getVersion")
                || method.getName().equals("getSysFields"))
                && !(Collection.class.isAssignableFrom(method.getReturnType()));
    }

    private static <E extends BaseEntity> Filter filterFromMethod(Method method, E entity) {
        String property = getPropertyFromMethod(method.getName());
        Object value;
        try {
            value = method.invoke(entity);
        } catch (Exception ignored) {
            // This does never happen because the methods always exist
            return null;
        }
        if (value == null)
            return null;
        return new Filter(property, value, false, AND);
    }

    @SuppressWarnings("unchecked")
    private static BooleanExpression handleCollection(PathBuilder<?> pb, BooleanExpression base, Filter filter) {
        Object o = filter.getValue();
        CollectionPath path = pb.getCollection(filter.getProperty(), o.getClass());
        switch (filter.getOperator()) {
            case AND:
                base = base.and(findNegation(path.any().eq(o), filter)); // FIXME
                break;
            case OR:
                base = base.or(findNegation(path.any().eq(o), filter));
                break;
        }
        return base;
    }

    private static BooleanExpression handleString(PathBuilder<?> pb, BooleanExpression base, Filter filter) {
        String s = filter.getValue().toString();
        boolean startsWith = s.endsWith("%");
        boolean endsWith = s.startsWith("%");
        StringPath sp = pb.getString(filter.getProperty());
        if (startsWith) {
            s = s.substring(0, s.length() - 1);
        }
        if (endsWith) {
            s = s.substring(1);
        }

        switch (filter.getOperator()) {
            case AND:
                if (startsWith && endsWith) {
                    base = base.and(findNegation(sp.containsIgnoreCase(s), filter));
                } else if (startsWith) {
                    base = base.and(findNegation(sp.startsWithIgnoreCase(s), filter));
                } else if (endsWith) {
                    base = base.and(findNegation(sp.endsWithIgnoreCase(s), filter));
                } else {
                    base = base.and(findNegation(sp.equalsIgnoreCase(s), filter));
                }
                break;
            case OR:
                if (startsWith && endsWith) {
                    base = base.or(findNegation(sp.containsIgnoreCase(s), filter));
                } else if (startsWith) {
                    base = base.or(findNegation(sp.startsWithIgnoreCase(s), filter));
                } else if (endsWith) {
                    base = base.or(findNegation(sp.endsWithIgnoreCase(s), filter));
                } else {
                    base = base.or(findNegation(sp.equalsIgnoreCase(s), filter));
                }
                break;
        }
        return base;
    }

    private static <E extends BaseEntity> BooleanExpression handleEntity(PathBuilder<?> pb, BooleanExpression base,
                                                                         Filter filter, Field field) {
        Object o = filter.getValue();
        if (o instanceof BaseEntity) {
            if (o.getClass().equals(field.getType())) {
                if (((BaseEntity) o).getId() != 0) {
                    base = handleOther(pb, base, filter);
                } else {
                    @SuppressWarnings("unchecked")
                    List<Filter> subFilters = getFiltersByEntity((Class<E>) o.getClass(), (E) o);
                    BooleanExpression partial = pb.get(filter.getProperty()).isNotNull();
                    for (Filter sf : subFilters) {
                        partial = partial.and(pb.get(filter.getProperty()).get(sf.getProperty()).eq(sf.getValue()));
                    }

                    switch (filter.getOperator()) {
                        case AND:
                            base = base.and(findNegation(partial, filter));
                            break;
                        case OR:
                            base = base.or(findNegation(partial, filter));
                            break;
                    }
                }
            } else {
                logger.error("An invalid value was passed to the field " + filter.getProperty() + ": expected "
                        + field.getType().getName() + ", got " + filter.getValue().getClass().getName());
            }
        }
        return base;
    }

    private static BooleanExpression handleOther(PathBuilder<?> pb, BooleanExpression base, Filter filter) {
        switch (filter.getOperator()) {
            case AND:
                return base.and(findNegation(pb.get(filter.getProperty()).eq(filter.getValue()), filter));
            case OR:
                return base.or(findNegation(pb.get(filter.getProperty()).eq(filter.getValue()), filter));
        }
        return base;
    }

    private static BooleanExpression handleBetween(PathBuilder<?> pb, BooleanExpression base, Filter filter,
                                                   Collection<Filter> filters) {

        String fromToField = filter.getProperty();
        final String fieldName = getBetweenFieldName(fromToField);
        Object fromValue = null;
        Object toValue = null;

        if (fromToField.toLowerCase().endsWith("from")) {
            fromValue = filter.getValue();
            Optional<Filter> toFilter = filters.stream().filter(f -> f.getProperty().equalsIgnoreCase(fieldName + "to"))
                    .findFirst();

            if (toFilter.isPresent())
                toValue = toFilter.get().getValue();
        } else {
            toValue = filter.getValue();

            Optional<Filter> fromFilter = filters.stream()
                    .filter(f -> f.getProperty().equalsIgnoreCase(fieldName + "from")).findFirst();

            if (fromFilter.isPresent())
                fromValue = fromFilter.get().getValue();
        }

        Object fromToValue = (fromValue != null ? fromValue : toValue);

        assert fromToValue != null;
        if (fromToValue.getClass().isAssignableFrom(Calendar.class)
                || fromToValue.getClass().isAssignableFrom(Date.class)) {
            Calendar f = ensureIsCalendar(fromValue);
            Calendar t = endOfDay(ensureIsCalendar(toValue));
            switch (filter.getOperator()) {
                case AND:
                    return base.and(findNegation(pb.getDate(fieldName, Calendar.class).between(f, t), filter));
                case OR:
                    return base.or(findNegation(pb.getDate(fieldName, Calendar.class).between(f, t), filter));
            }
        } else {
            Comparable<?> f = (Comparable<?>) fromValue;
            Comparable<?> t = (Comparable<?>) toValue;
            switch (filter.getOperator()) {
                case AND:
                    return base.and(findNegation(pb.getComparable(fieldName, Comparable.class).between(f, t), filter));
                case OR:
                    return base.or(findNegation(pb.getComparable(fieldName, Comparable.class).between(f, t), filter));
            }
        }

        return base;
    }

    private static Calendar ensureIsCalendar(Object object) {
        if (object instanceof Date)
            return fromDate((Date) object);
        return (Calendar) object;
    }

    private static String getBetweenFieldName(String field) {
        if (field.toLowerCase().endsWith("from")) {
            return field.substring(0, field.length() - "from".length());
        } else if (field.toLowerCase().endsWith("to")) {
            return field.substring(0, field.length() - "to".length());
        } else {
            return field;
        }
    }

    private static BooleanExpression findNegation(BooleanExpression expr, Filter filter) {
        if (filter.isNegative())
            return expr.not();
        return expr;
    }
}
