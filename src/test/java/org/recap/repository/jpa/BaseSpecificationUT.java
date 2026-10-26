package org.recap.repository.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;
import org.recap.repository.BaseSpecification;
import org.recap.repository.SearchCriteria;
import org.recap.repository.SearchOperation;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

public class BaseSpecificationUT extends BaseTestCaseUT {


    @Mock
    SearchCriteria criteria;

    BaseSpecification baseSpecification = new BaseSpecification() {
    };

    @Mock
    Root root;

    @Mock
    CriteriaQuery<?> criteriaQuery;

    @Mock
    CriteriaBuilder criteriaBuilder;

    @Mock
    Specification specification;


    List<SearchCriteria> list = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(baseSpecification, "list", list);
    }

    @Test
    public void toPredicateGreaterThan() {
        baseSpecification.add(criteria);
        Mockito.when(criteria.getOperation()).thenReturn(SearchOperation.GREATER_THAN);
        Mockito.when(criteria.getKey()).thenReturn("SearchKey");
        Mockito.when(criteria.getValue()).thenReturn("SearchValue");
        Predicate predicate = baseSpecification.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    @Test
    public void toPredicateLesserThan() {
        baseSpecification.add(criteria);
        Mockito.when(criteria.getOperation()).thenReturn(SearchOperation.LESS_THAN);
        Mockito.when(criteria.getKey()).thenReturn("SearchKey");
        Mockito.when(criteria.getValue()).thenReturn("SearchValue");
        Predicate predicate = baseSpecification.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    @Test
    public void toPredicateGreaterThanEqual() {
        baseSpecification.add(criteria);
        Mockito.when(criteria.getOperation()).thenReturn(SearchOperation.GREATER_THAN_EQUAL);
        Mockito.when(criteria.getKey()).thenReturn("SearchKey");
        Mockito.when(criteria.getValue()).thenReturn("SearchValue");
        Predicate predicate = baseSpecification.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    @Test
    public void toPredicateLesserThanEqual() {
        baseSpecification.add(criteria);
        Mockito.when(criteria.getOperation()).thenReturn(SearchOperation.LESS_THAN_EQUAL);
        Mockito.when(criteria.getKey()).thenReturn("SearchKey");
        Mockito.when(criteria.getValue()).thenReturn("SearchValue");
        Predicate predicate = baseSpecification.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    @Test
    public void toPredicateNotEqual() {
        baseSpecification.add(criteria);
        Mockito.when(criteria.getOperation()).thenReturn(SearchOperation.NOT_EQUAL);
        Mockito.when(criteria.getKey()).thenReturn("SearchKey");
        Mockito.when(criteria.getValue()).thenReturn("SearchValue");
        Predicate predicate = baseSpecification.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    @Test
    public void toPredicateEqual() {
        baseSpecification.add(criteria);
        Mockito.when(criteria.getOperation()).thenReturn(SearchOperation.EQUAL);
        Mockito.when(criteria.getKey()).thenReturn("SearchKey");
        Mockito.when(criteria.getValue()).thenReturn("SearchValue");
        Predicate predicate = baseSpecification.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    @Test
    public void toPredicateContains() {
        baseSpecification.add(criteria);
        Mockito.when(criteria.getOperation()).thenReturn(SearchOperation.CONTAINS);
        Mockito.when(criteria.getKey()).thenReturn("SearchKey");
        Mockito.when(criteria.getValue()).thenReturn("SearchValue");
        Predicate predicate = baseSpecification.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    @Test
    public void toPredicateStartsWith() {
        baseSpecification.add(criteria);
        Mockito.when(criteria.getOperation()).thenReturn(SearchOperation.STARTS_WITH);
        Mockito.when(criteria.getKey()).thenReturn("SearchKey");
        Mockito.when(criteria.getValue()).thenReturn("SearchValue");
        Predicate predicate = baseSpecification.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    @Test
    public void toPredicateEndsWith() {
        baseSpecification.add(criteria);
        Mockito.when(criteria.getOperation()).thenReturn(SearchOperation.ENDS_WITH);
        Mockito.when(criteria.getKey()).thenReturn("SearchKey");
        Mockito.when(criteria.getValue()).thenReturn("SearchValue");
        Predicate predicate = baseSpecification.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    @Test
    public void toPredicateIn() {
        baseSpecification.add(criteria);
        Mockito.when(criteria.getOperation()).thenReturn(SearchOperation.IN);
        Mockito.when(criteria.getKey()).thenReturn("SearchKey");
        Mockito.when(criteria.getValue()).thenReturn("SearchValue");
        try {
            Predicate predicate = baseSpecification.toPredicate(root, criteriaQuery, criteriaBuilder);
        } catch (Exception e) {
        }
    }

    @Test
    public void toPredicateNotIn() {
        baseSpecification.add(criteria);
        Mockito.when(criteria.getOperation()).thenReturn(SearchOperation.NOT_IN);
        Mockito.when(criteria.getKey()).thenReturn("SearchKey");
        Mockito.when(criteria.getValue()).thenReturn("SearchValue");
        try {
            Predicate predicate = baseSpecification.toPredicate(root, criteriaQuery, criteriaBuilder);
        } catch (Exception e) {
        }
    }
}
