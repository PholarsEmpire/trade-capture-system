package com.technicalchallenge.rsql;

import cz.jirutka.rsql.parser.ast.*;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;

// Custom RSQL Visitor to convert RSQL AST (Abstract Syntax Tree) nodes into JPA Specifications
// This class extends AbstractRSQLVisitor and implements methods to handle AND, OR, and Comparison nodes
// It builds Specifications that can be used to query the database based on RSQL queries
public class RsqlVisitor<T> implements RSQLVisitor<Specification<T>, Void> {

    @Override
    public Specification<T> visit(AndNode node, Void param) {
        return node.getChildren().stream()
                .map(n -> n.accept(this,null))
                .reduce(Specification::and)
                .orElse(null);
    }

    @Override
    public Specification<T> visit(OrNode node, Void param) {
        return node.getChildren().stream()
                .map(n -> n.accept(this,null))
                .reduce(Specification::or)
                .orElse(null);
    }


    // @Override
    // public Specification<T> visit(ComparisonNode node, Void param) {
    //     return (root, query, cb) -> {
    //         String selector = node.getSelector();
    //         String argument = node.getArguments().get(0);
    //         Path<?> path = getPath(root, selector);

    //         Class<?> type = path.getJavaType();

    //         switch (node.getOperator().getSymbol()) {
    //             case "==": return cb.equal(path, convert(argument, type));
    //             case "!=": return cb.notEqual(path, convert(argument, type));
    //             case "=gt=": return cb.greaterThan(path.as(type), (Comparable<?>) convert(argument, type));
    //             case "=lt=": return cb.lessThan(path.as(type), (Comparable<?>) convert(argument, type));
    //             case "=ge=": return cb.greaterThanOrEqualTo(path.as(type), (Comparable<?>) convert(argument, type));
    //             case "=le=": return cb.lessThanOrEqualTo(path.as(type), (Comparable<?>) convert(argument, type));
    //             default: throw new IllegalArgumentException("Unsupported operator: " + node.getOperator());
    //         }
    //     };
    // }





    @Override
    public Specification<T> visit(ComparisonNode node, Void param) {
        return (root, query, cb) -> {
            String selector = node.getSelector();
            String argument = node.getArguments().get(0);
            Path<?> path = getPath(root, selector);

            Class<?> type = path.getJavaType();

            switch (node.getOperator().getSymbol()) {
                case "==":
                    return cb.equal(path, convert(argument, type));
                case "!=":
                    return cb.notEqual(path, convert(argument, type));
                case "=gt=":
                    if (type.equals(Integer.class)) {
                        return cb.greaterThan((Path<Integer>) path, Integer.valueOf(argument));
                    } else if (type.equals(Long.class)) {
                        return cb.greaterThan((Path<Long>) path, Long.valueOf(argument));
                    } else if (type.equals(Double.class)) {
                        return cb.greaterThan((Path<Double>) path, Double.valueOf(argument));
                    } else if (Comparable.class.isAssignableFrom(type)) {
                        return cb.greaterThan((Path<Comparable>) path, (Comparable) convert(argument, type));
                    } else {
                        throw new IllegalArgumentException("Field " + selector + " is not Comparable");
                    }
                case "=lt=":
                    if (type.equals(Integer.class)) {
                        return cb.lessThan((Path<Integer>) path, Integer.valueOf(argument));
                    } else if (type.equals(Long.class)) {
                        return cb.lessThan((Path<Long>) path, Long.valueOf(argument));
                    } else if (type.equals(Double.class)) {
                        return cb.lessThan((Path<Double>) path, Double.valueOf(argument));
                    } else if (Comparable.class.isAssignableFrom(type)) {
                        return cb.lessThan((Path<Comparable>) path, (Comparable) convert(argument, type));
                    } else {
                        throw new IllegalArgumentException("Field " + selector + " is not Comparable");
                    }
                case "=ge=":
                    if (type.equals(Integer.class)) {
                        return cb.greaterThanOrEqualTo((Path<Integer>) path, Integer.valueOf(argument));
                    } else if (type.equals(Long.class)) {
                        return cb.greaterThanOrEqualTo((Path<Long>) path, Long.valueOf(argument));
                    } else if (type.equals(Double.class)) {
                        return cb.greaterThanOrEqualTo((Path<Double>) path, Double.valueOf(argument));
                    } else if (Comparable.class.isAssignableFrom(type)) {
                        return cb.greaterThanOrEqualTo((Path<Comparable>) path, (Comparable) convert(argument, type));
                    } else {
                        throw new IllegalArgumentException("Field " + selector + " is not Comparable");
                    }
                case "=le=":
                    if (type.equals(Integer.class)) {
                        return cb.lessThanOrEqualTo((Path<Integer>) path, Integer.valueOf(argument));
                    } else if (type.equals(Long.class)) {
                        return cb.lessThanOrEqualTo((Path<Long>) path, Long.valueOf(argument));
                    } else if (type.equals(Double.class)) {
                        return cb.lessThanOrEqualTo((Path<Double>) path, Double.valueOf(argument));
                    } else if (Comparable.class.isAssignableFrom(type)) {
                        return cb.lessThanOrEqualTo((Path<Comparable>) path, (Comparable) convert(argument, type));
                    } else {
                        throw new IllegalArgumentException("Field " + selector + " is not Comparable");
                    }
                default:
                    throw new IllegalArgumentException("Unsupported operator: " + node.getOperator());
            }
        };
    }


// Helper method to convert String arguments to the appropriate type
    private Object convert(String value, Class<?> type) {
        if (type.equals(Integer.class)) return Integer.valueOf(value);
        if (type.equals(Long.class)) return Long.valueOf(value);
        if (type.equals(Double.class)) return Double.valueOf(value);
        if (type.equals(Boolean.class)) return Boolean.valueOf(value);
        if (type.equals(LocalDate.class)) return LocalDate.parse(value);
        // Add more types as needed
        return value; // default to String
    }

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
