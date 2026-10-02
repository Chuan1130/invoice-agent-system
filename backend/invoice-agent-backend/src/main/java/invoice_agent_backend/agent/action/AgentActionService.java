package invoice_agent_backend.agent.action;

import invoice_agent_backend.agent.model.AgentActionConfirmRequest;
import invoice_agent_backend.agent.model.AgentActionExecutionResult;
import invoice_agent_backend.agent.model.AgentPendingActionView;

public interface AgentActionService {

    AgentActionRequestRecord prepareHumanReview(
            String requestId,
            Long taskId,
            String decision,
            String proposedComment
    );

    AgentActionExecutionResult confirmHumanReview(
            String actionToken,
            AgentActionConfirmRequest request
    );

    AgentPendingActionView getAction(
            String actionToken
    );

    AgentPendingActionView cancelAction(
            String actionToken
    );

    void cancelPendingByRequestId(
            String requestId
    );
}
