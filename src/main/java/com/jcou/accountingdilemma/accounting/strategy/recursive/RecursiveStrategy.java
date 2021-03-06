package com.jcou.accountingdilemma.accounting.strategy.recursive;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import com.jcou.accountingdilemma.accounting.DuePayment;
import com.jcou.accountingdilemma.accounting.Solution;
import com.jcou.accountingdilemma.accounting.strategy.Strategy;
import com.jcou.accountingdilemma.io.input.InputFileData;

import org.apache.log4j.Logger;

/**
 *  Idea of the algorithm:
 * 
 *  Recursive function: args = BankTransferAmount, DuePayments list, DuePayments Sum list
 *  Checking if there are only positive values
 *  Sorting the list with ascending order
 *  Parse DuePayments list
 *      Remove the current DuePayment from the function DuePayments clone
 *      Clone DuePayments Sum list
 *      Add the current DuePayment to the loop DuePayments Sum clone
 *      Check if the loop DuePayments Sum clone is a solution
 *          Yes -> return the solution
*           No -> check if the list contains only positive value and the sum > bank transfer
                Yes -> Continue to next loop
 *      Continue to explore the tree of possibilities (deep-first search)
 *          Using the function DuePayments clone and the loop DuePayments Sum clone
 *      Check if a solution has been returned from the deep-first search
 *          Yes -> return the solution
 * 
 * ============================================================================
 * 
 * Flat example with the following arguments (for any values even negatives):
 * -BankTransfer impossible to find
 * -DuePayments list: [A, B]
 *  
 *  Initially:
 *  DuePaymentsFunction=[A, B]
 *  DuePaymentsSumFunction=[]
 * 
 *  Loop 1
 *      Remove A => DuePaymentsFunction=[B]
 *      Clone DuePaymentsSumFunction=[] as DuePaymentsSumLoop=[]
 *      Add A to DuePaymentsSumLoop => DuePaymentsSumLoop=[A]
 *      Check solution found ? NO
 *      Explore deeper with DuePaymentsFunction=[B] and DuePaymentsSumFunction=[A]
 *          Function start
 *              DuePaymentsFunction=[B]
 *              DuePaymentsSumFunction=[A]
 * 
 *              Loop 1
 *                  Remove B => DuePaymentsFunction=[]
 *                  Clone DuePaymentsSumFunction=[A] as DuePaymentsSumLoop=[A]
 *                  Add B to DuePaymentsSumLoop => DuePaymentsSumLoop=[A, B]
 *                  Check solution found ? NO
 *                  Explore deeper with DuePaymentsFunction=[] and DuePaymentsSumFunction=[A, B]
 *                      DuePaymentsFunction Empty
 *              End Loop
 *          End Function
 *  Loop 2
 *      Remove B => DuePaymentsFunction=[]
 *      Clone DuePaymentsSumFunction=[] as DuePaymentsSumLoop=[]
 *      Add B to DuePaymentsSumLoop => DuePaymentsSumLoop=[B]
 *      Check solution found ? NO
 *      Explore deeper with DuePaymentsFunction=[] and DuePaymentsSumFunction=[B]
 *          DuePaymentsFunction Empty
 *  End Loop
 *      
 *      
 */
public class RecursiveStrategy implements Strategy {
    private static final Logger logger = 
        Logger.getLogger(RecursiveStrategy.class);
    
    private boolean onlyPositiveValues = false;

    /**
     * Find the list of DuePayments which correspond to the BankTransfer
     * @param inputFileData InputFileData
     * @return the solution if one is found, null otherwise
     */
    public Solution findSolution(InputFileData inputFileData) {
        logger.debug("Finding a solution");
        validateInputFileData(inputFileData);

        BigDecimal bankTransferAmount = inputFileData.getBankTransfer().getAmount();
        List<DuePayment> duePaymentsList = inputFileData.getDuePaymentsList();

        onlyPositiveValues = duePaymentsList.stream()
                .allMatch(duePayment -> duePayment.getAmount().compareTo(BigDecimal.ZERO) >= 0)
            && bankTransferAmount.compareTo(BigDecimal.ZERO) > 0;
        
        // Ordering the list in case there are only positive values (Ascending)
        // Also Summing big values first may not be the best idea to find the answer quickly
        duePaymentsList = duePaymentsList.stream()
            .sorted((dp1, dp2) -> dp1.getAmount().compareTo(dp2.getAmount()))
            .collect(Collectors.toList());

        Solution solution = this.explorePossibilities(bankTransferAmount, duePaymentsList);
        if (solution == null) {
            logger.debug("Solution search ended without result");
        } else {
            logger.debug("Solution search ended with a result");
        }

        return solution;
    }

    /**
     * Explores all the possibilies for a given DuePayments list
     * @param bankTransferAmount BankTransfer amount
     * @param duePaymentsList list of DuePayments
     * @return the solution if one is found, null otherwise
     */
    private Solution explorePossibilities(BigDecimal bankTransferAmount, 
            List<DuePayment> duePaymentsList) {
        // Resolving trivial case: the BankTransfer is a single DuePayment
        for (DuePayment duePayment: duePaymentsList) {
            if (isSolutionFound(bankTransferAmount, duePayment.getAmount())) {
                return new Solution(Arrays.asList(duePayment));
            }
        }

        List<DuePayment> duePaymentsSumList = new ArrayList<>();

        return explore(bankTransferAmount, duePaymentsList, duePaymentsSumList);
    }

    private Solution explore(BigDecimal bankTransferAmount,
            List<DuePayment> duePaymentsList,
            List<DuePayment> duePaymentsSumList) {
        // Making a save of the lists of DuePayments for the current function execution
        List<DuePayment> localDuePaymentsList = cloneDuePaymentsList(duePaymentsList);
        ListIterator<DuePayment> duePaymentsListIte = localDuePaymentsList.listIterator();
        while(duePaymentsListIte.hasNext()) {
            // Removing a DuePayment from the function DuePaymentsList clone
            DuePayment duePayment = duePaymentsListIte.next();
            duePaymentsListIte.remove();

            // Cloning DuePaymentsList Sum for next loop
            List<DuePayment> nextDuePaymentsSumList = cloneDuePaymentsList(duePaymentsSumList);
            // Adding the current DuePayment to the loop DuePaymentsList Sum clone
            nextDuePaymentsSumList.add(duePayment);
            // Checking if a solution was found
            BigDecimal duePaymentsSum = duePaymentsSum(nextDuePaymentsSumList);
            if (isSolutionFound(bankTransferAmount, duePaymentsSum)) {
                return new Solution(nextDuePaymentsSumList);
            } else if (this.onlyPositiveValues && 
                    duePaymentsSum.compareTo(bankTransferAmount) > 0) {
                // if the list contains only positive values there is no need to go deeper
                continue;
            }

            // Deep-first search to find the solution
            Solution solution = explore(bankTransferAmount, localDuePaymentsList, nextDuePaymentsSumList);
            // Checking if a solution was found during the exploration
            if (solution != null) {
                return solution;
            }
            // No solution found
            // Removing another DuePayment from the function DuePaymentsList clone if possible
        }

        return null;
    }

    /**
     * Checks if the solution proposed is correct
     * @param bankTransferAmount BankTransfer amount
     * @param solutionAmount solution amount
     * @return true if the solution is correct false otherwise
     */
    private boolean isSolutionFound(BigDecimal bankTransferAmount, BigDecimal solutionAmount) {
        return bankTransferAmount.equals(solutionAmount);
    }

    /**
     * Sums all the Due Payment amounts present in a list
     * @param duePaymentsSumList list of DuePayments
     * @return sum of all the Due Payment amounts present in the list
     */
    private BigDecimal duePaymentsSum(List<DuePayment> duePaymentsSumList) {
        return duePaymentsSumList.stream()
            .map(DuePayment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Validates the InputFileData input
     */
    private void validateInputFileData(InputFileData inputFileData) {
        if (inputFileData == null) {
            throw new IllegalArgumentException("Input file data must not be null");
        } else {
            inputFileData.validate();
        }
    }

    /**
     * Clone a DuePayments list
     * @param duePaymentsList list of DuePayments
     * @return A clone of the list of DuePayments provided
     */
    private List<DuePayment> cloneDuePaymentsList(List<DuePayment> duePaymentsList) {
        return duePaymentsList.stream()
            .map(DuePayment::clone)
            .collect(Collectors.toList());
    }
}