package com.technicalchallenge.specifications;

import com.technicalchallenge.model.Trade;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;


//This is a helper class to build dynamic queries for filtering trades based on various optional criteria.
// Each method returns a Specification (a re-usable filter) that can be combined to form complex queries.
// If a filter parameter is null or empty or not provided, the corresponding Specification returns a no-op (conjunction) meaning it does not affect the query.
// This approach allows for flexible and reusable query construction in the service layer.
// Specifications can be combined using the `and` and `or` methods provided by the Specification interface.
// Example usage in service layer:
// Specification<Trade> spec = Specification.where(TradeSpecifications.hasCounterparty(counterpartyName))
public class TradeSpecifications {

    // Counterparty name filter (e.g. "MegaFund", "BigBank")
    public static Specification<Trade> hasCounterparty(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) return cb.conjunction();
            return cb.equal(cb.lower(root.join("counterparty").get("name")), name.toLowerCase());
        };
    }

    // Book name filter (e.g. "FX-BOOK-1")
    public static Specification<Trade> hasBook(String bookName) {
        return (root, query, cb) -> {
            if (bookName == null || bookName.isBlank()) return cb.conjunction();
            return cb.equal(cb.lower(root.join("book").get("bookName")), bookName.toLowerCase());
        };
    }

    // Trade status filter (e.g. "NEW", "AMENDED", "CANCELLED")
    public static Specification<Trade> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) return cb.conjunction();
            return cb.equal(cb.lower(root.join("tradeStatus").get("tradeStatus")), status.toLowerCase());
        };
    }


    // Trader user ID filter
    public static Specification<Trade> hasTrader(Long traderId) {
        return (root, query, cb) -> {
            if (traderId == null) return cb.conjunction();
            return cb.equal(root.join("traderUser").get("id"), traderId);
        };
    }

    // Date range filter for tradeDate (date agreement was made, not the start date or date of execution)
    // If both from and to are provided, filter between them
    // If only from is provided, filter from that date onwards
    // If only to is provided, filter up to that date
    public static Specification<Trade> dateBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from != null && to != null)
                return cb.between(root.get("tradeDate"), from, to);
            if (from != null)
                return cb.greaterThanOrEqualTo(root.get("tradeDate"), from);
            if (to != null)
                return cb.lessThanOrEqualTo(root.get("tradeDate"), to);
            return cb.conjunction();
        };
    }
}
