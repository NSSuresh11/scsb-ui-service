package org.recap.repository;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

/**
 * BaseSpecification which defines the criteria query for the Domain Driven
 *
 * @author Ramanujam (CEI)
 *
 */

public abstract class BaseSpecification<T> implements Specification<T> {

  private List<SearchCriteria> list;

  public BaseSpecification() {
    this.list = new ArrayList<>();
  }

  /**
   * Adds the criteria to be used in the query.
   * @param criteria
   */
  public void add(SearchCriteria criteria) {
    list.add(criteria);
  }

  @Override
  public Predicate toPredicate(Root<T> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
    //create a new predicate list
    List<Predicate> predicates = new ArrayList<>();


    //add add criteria to predicates
    list.forEach(criteria -> {
      if (criteria.getOperation().equals(SearchOperation.GREATER_THAN)) {
        predicates.add(criteriaBuilder.greaterThan(
                root.get(criteria.getKey()), criteria.getValue().toString()));
      } else if (criteria.getOperation().equals(SearchOperation.LESS_THAN)) {
        predicates.add(criteriaBuilder.lessThan(
                root.get(criteria.getKey()), criteria.getValue().toString()));
      } else if (criteria.getOperation().equals(SearchOperation.GREATER_THAN_EQUAL)) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                root.get(criteria.getKey()), criteria.getValue().toString()));
      } else if (criteria.getOperation().equals(SearchOperation.LESS_THAN_EQUAL)) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                root.get(criteria.getKey()), criteria.getValue().toString()));
      } else if (criteria.getOperation().equals(SearchOperation.NOT_EQUAL)) {
        predicates.add(criteriaBuilder.notEqual(
                root.get(criteria.getKey()), criteria.getValue()));
      } else if (criteria.getOperation().equals(SearchOperation.EQUAL)) {
        predicates.add(criteriaBuilder.equal(
                root.get(criteria.getKey()), criteria.getValue()));
      } else if (criteria.getOperation().equals(SearchOperation.CONTAINS)) {
        predicates.add(criteriaBuilder.like(
                criteriaBuilder.lower(root.get(criteria.getKey())),
                "%" + criteria.getValue().toString().toLowerCase() + "%"));
      } else if (criteria.getOperation().equals(SearchOperation.STARTS_WITH)) {
        predicates.add(criteriaBuilder.like(
                criteriaBuilder.lower(root.get(criteria.getKey())),
                criteria.getValue().toString().toLowerCase() + "%"));
      } else if (criteria.getOperation().equals(SearchOperation.ENDS_WITH)) {
        predicates.add(criteriaBuilder.like(
                criteriaBuilder.lower(root.get(criteria.getKey())),
                "%" + criteria.getValue().toString().toLowerCase()));
      } else if (criteria.getOperation().equals(SearchOperation.IN)) {
        predicates.add(criteriaBuilder.in(root.get(criteria.getKey())).value(criteria.getValue()));
      } else if (criteria.getOperation().equals(SearchOperation.NOT_IN)) {
        predicates.add(criteriaBuilder.not(root.get(criteria.getKey())).in(criteria.getValue()));
      }
    });


    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
  }
}

