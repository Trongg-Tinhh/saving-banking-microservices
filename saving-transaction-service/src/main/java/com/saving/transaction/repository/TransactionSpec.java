package com.saving.transaction.repository;

import com.saving.transaction.entity.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification builder cho Transaction — tránh lỗi PostgreSQL
 * "could not determine data type of parameter $N" khi dùng JPQL với null.
 *
 * Chỉ thêm điều kiện vào WHERE khi tham số khác null.
 */
public class TransactionSpec {

    private TransactionSpec() {}

    public static Specification<Transaction> withFilters(
            String contractNo,
            String cif,
            String txType,
            String status,
            OffsetDateTime fromDate,
            OffsetDateTime toDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (contractNo != null && !contractNo.isBlank()) {
                predicates.add(cb.equal(root.get("contractNo"), contractNo));
            }
            if (cif != null && !cif.isBlank()) {
                predicates.add(cb.equal(root.get("cif"), cif));
            }
            if (txType != null && !txType.isBlank()) {
                predicates.add(cb.equal(root.get("transactionType"), txType));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
