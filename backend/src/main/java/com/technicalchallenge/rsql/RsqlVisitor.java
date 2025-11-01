package com.technicalchallenge.rsql;

import cz.jirutka.rsql.parser.ast.*;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;

/**
 * Custom RSQL Visitor to convert RSQL AST (Abstract Syntax Tree) nodes into JPA Specifications.
 * This class implements RSQLVisitor and handles AND, OR, and Comparison nodes.
 * It builds type-safe Specifications for database queries based on RSQL query strings.
 * 
 * Supported operators: ==, !=, =gt=, =lt=, =ge=, =le=
 * Supported types: String, Integer, Long, Double, Boolean, LocalDate
 */
public class RsqlVisitor<T> implements RSQLVisitor<Specification<T>, Void> {

    @Override
    public Specification<T> visit(AndNode node, Void param) {
        return node.getChildren().stream()
                .map(n -> n.accept(this, null))
                .reduce(Specification::and)
                .orElse(null);
    }

    @Override
    public Specification<T> visit(OrNode node, Void param) {
        return node.getChildren().stream()
                .map(n -> n.accept(this, null))
                .reduce(Specification::or)
                .orElse(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Specification<T> visit(ComparisonNode node, Void param) {
        return (root, query, cb) -> {
            String selector = node.getSelector();
            String argument = node.getArguments().get(0);
            Path<?> path = getPath(root, selector);
            Class<?> type = path.getJavaType();
            Object convertedValue = convert(argument, type);

            switch (node.getOperator().getSymbol()) {
                case "==":
                    return cb.equal(path, convertedValue);
                    
                case "!=":
                    return cb.notEqual(path, convertedValue);
                    
                case "=gt=":
                    return createComparison(cb, path, convertedValue, type, selector, 
                        (criteriaBuilder, p, v) -> criteriaBuilder.greaterThan((Expression<Comparable>) p, (Comparable) v));
                    
                case "=lt=":
                    return createComparison(cb, path, convertedValue, type, selector,
                        (criteriaBuilder, p, v) -> criteriaBuilder.lessThan((Expression<Comparable>) p, (Comparable) v));
                    
                case "=ge=":
                    return createComparison(cb, path, convertedValue, type, selector,
                        (criteriaBuilder, p, v) -> criteriaBuilder.greaterThanOrEqualTo((Expression<Comparable>) p, (Comparable) v));
                    
                case "=le=":
                    return createComparison(cb, path, convertedValue, type, selector,
                        (criteriaBuilder, p, v) -> criteriaBuilder.lessThanOrEqualTo((Expression<Comparable>) p, (Comparable) v));
                    
                default:
                    throw new IllegalArgumentException("Unsupported operator: " + node.getOperator());
            }
        };
    }

    /**
     * Helper method to create comparison predicates with proper type handling.
     * Suppresses warnings since the type checking is done at runtime.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate createComparison(
            CriteriaBuilder cb,
            Path<?> path,
            Object value,
            Class<?> type,
            String selector,
            ComparisonFunction function) {
        
        if (!Comparable.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Field '" + selector + "' of type " + type.getSimpleName() + " is not Comparable");
        }
        
        return function.apply(cb, (Expression<Comparable>) path, (Comparable) value);
    }

    /**
     * Functional interface for comparison operations.
     */
    @FunctionalInterface
    private interface ComparisonFunction {
        @SuppressWarnings("rawtypes")
        Predicate apply(CriteriaBuilder cb, Expression<Comparable> path, Comparable value);
    }

    /**
     * Converts string arguments to the appropriate Java type.
     * 
     * @param value The string value to convert
     * @param type The target type class
     * @return The converted value
     * @throws IllegalArgumentException if conversion fails
     */
    private Object convert(String value, Class<?> type) {
        try {
            if (type.equals(Integer.class)) {
                return Integer.valueOf(value);
            }
            if (type.equals(Long.class)) {
                return Long.valueOf(value);
            }
            if (type.equals(Double.class)) {
                return Double.valueOf(value);
            }
            if (type.equals(Boolean.class)) {
                return Boolean.valueOf(value);
            }
            if (type.equals(LocalDate.class)) {
                return LocalDate.parse(value);
            }
            // Default to String for unsupported types
            return value;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert value '" + value + "' to type " + type.getSimpleName(), e);
        }
    }

    /**
     * Navigates to the appropriate field path, handling nested properties with joins.
     * 
     * @param root The root entity
     * @param selector The field selector (supports dot notation for nested fields)
     * @return The path to the field
     */
    private Path<?> getPath(Root<T> root, String selector) {
        if (selector.contains(".")) {
            String[] parts = selector.split("\\.");
            Path<?> path = root.join(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                path = path.get(parts[i]);
            }
            return path;
        } else {
            return root.get(selector);
        }
    }
}
