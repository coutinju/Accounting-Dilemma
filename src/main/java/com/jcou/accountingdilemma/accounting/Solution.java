package com.jcou.accountingdilemma.accounting;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Immutable class representing a solution to the 'Accounting Dilemma'
 */
public class Solution {
    private static final String SEPARATOR = " ";

    /**
     * 'Due Payment's list whose sum is equal to the amount of the'Bank Transfer'
     */
    private List<DuePayment> duePaymentsToSumList;

    public Solution(List<DuePayment> duePaymentsToSumList) {
        this.duePaymentsToSumList = duePaymentsToSumList.stream().map(DuePayment::clone)
            .collect(Collectors.toList());
    }

    public List<DuePayment> getDuePaymentsToSumList() {
        return this.duePaymentsToSumList.stream().map(DuePayment::clone)
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return duePaymentsToSumList.stream()
            .map(DuePayment::toString)
            .collect(Collectors.joining(SEPARATOR));
    }
}