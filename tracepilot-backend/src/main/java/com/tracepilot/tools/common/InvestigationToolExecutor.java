
package com.tracepilot.tools.common;

import com.tracepilot.investigation.domain.InvestigationBudget;
import com.tracepilot.investigation.domain.InvestigationLimitExceededException;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Component
public class InvestigationToolExecutor {

    public <T> ToolResult<T> execute(
            InvestigationBudget budget,
            Supplier<List<T>> operation) {

        Objects.requireNonNull(budget, "budget is required");
        Objects.requireNonNull(operation, "operation is required");

        // Do not consume a budget violation as ordinary tool data.
        budget.claimToolCall();

        try {
            List<T> results = operation.get();

            if (budget.isExpired()) {
                throw new InvestigationLimitExceededException(
                        "Investigation deadline exceeded"
                );
            }

            if (results == null) {
                return ToolResult.error(
                        "Tool returned an invalid response"
                );
            }

            return ToolResult.success(results);

        } catch (InvestigationLimitExceededException ex) {
            throw ex;

        } catch (IllegalArgumentException ex) {
            return ToolResult.invalid(
                    "Invalid tool arguments"
            );

        } catch (RuntimeException ex) {
            // Log the exception internally before returning
            // a sanitized error to the agent.
            return ToolResult.error(
                    "Tool execution failed"
            );
        }
    }
}
