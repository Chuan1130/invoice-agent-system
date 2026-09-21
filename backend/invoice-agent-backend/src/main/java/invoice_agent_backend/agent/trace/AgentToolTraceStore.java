package invoice_agent_backend.agent.trace;

import invoice_agent_backend.mapper.AgentToolTraceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Persists only tool summaries, independently of invoice workflow transactions. */
@Service
public class AgentToolTraceStore {
    private final AgentToolTraceMapper mapper;

    public AgentToolTraceStore(AgentToolTraceMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(String requestId, List<AgentToolTrace> traces) {
        for (int i = 0; i < traces.size(); i++) {
            mapper.insert(requestId, i + 1, traces.get(i));
        }
    }
}
