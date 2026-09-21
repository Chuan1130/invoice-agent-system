-- Apply to the existing application database before enabling the Supervisor.
-- Manual additive migration; the application does not run this automatically.
CREATE TABLE IF NOT EXISTS agent_tool_trace (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(36) NOT NULL,
    sequence_no INT NOT NULL,
    tool_name VARCHAR(100) NOT NULL,
    input_summary VARCHAR(503),
    result_summary VARCHAR(503),
    success BOOLEAN NOT NULL,
    called_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_agent_trace_request_sequence (request_id, sequence_no)
);
