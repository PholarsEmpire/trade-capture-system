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
        // Step 1: Parse (meaning transform) RSQL string into an AST (Abstract Syntax Tree, which is a tree representation of the syntax structure)
        Node rootNode = new RSQLParser().parse(query);
        // Step 2: Visit the AST and convert to Specification
        return rootNode.accept(new RsqlVisitor<T>(),null);
    }
}
