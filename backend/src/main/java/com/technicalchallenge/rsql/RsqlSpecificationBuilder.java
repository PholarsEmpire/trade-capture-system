package com.technicalchallenge.rsql;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import org.springframework.data.jpa.domain.Specification;

// Simple RSQL Specification Builder to convert RSQL query strings into JPA Specifications
// This class uses the RSQLParser to parse the query and a custom RsqlVisitor to build the Specification
public class RsqlSpecificationBuilder<T> {
    public Specification<T> parse(String query) {
        if (query == null || query.isBlank()) {
            return (root, criteriaQuery, cb) -> cb.conjunction();
        }
        Node rootNode = new RSQLParser().parse(query);
        return rootNode.accept(new RsqlVisitor<T>(),null);
    }
}
