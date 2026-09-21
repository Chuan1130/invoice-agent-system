package invoice_agent_backend.agent.trace;

import invoice_agent_backend.mapper.AgentToolTraceMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(AgentToolTraceStoreTest.Config.class)
class AgentToolTraceStoreTest {
    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:agent_trace;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean PlatformTransactionManager transactionManager(DataSource ds) {
            return new DataSourceTransactionManager(ds);
        }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource ds) throws Exception {
            var factory = new SqlSessionFactoryBean();
            factory.setDataSource(ds);
            var sessionFactory = factory.getObject();
            sessionFactory.getConfiguration().addMapper(AgentToolTraceMapper.class);
            return sessionFactory;
        }
        @Bean AgentToolTraceMapper mapper(SqlSessionFactory factory) {
            return new SqlSessionTemplate(factory).getMapper(AgentToolTraceMapper.class);
        }
        @Bean AgentToolTraceStore store(AgentToolTraceMapper mapper) {
            return new AgentToolTraceStore(mapper);
        }
    }

    @Autowired DataSource dataSource;
    @Autowired AgentToolTraceStore store;
    @Autowired PlatformTransactionManager transactionManager;
    private JdbcTemplate jdbc;

    @BeforeEach void setUp() {
        new ResourceDatabasePopulator(new ClassPathResource("db/manual/001_agent_tool_trace.sql"))
                .execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("DELETE FROM agent_tool_trace");
    }

    private AgentToolTrace trace(boolean success) {
        return new AgentToolTrace("getRuleHitsTool", "taskId=6", "summary", success,
                LocalDateTime.of(2026, 9, 21, 10, 0));
    }

    @Test void persistsOrderedSuccessAndFailureSummaries() {
        store.save("request-a", List.of(trace(true), trace(false)));
        var rows = jdbc.queryForList("SELECT * FROM agent_tool_trace WHERE request_id=? ORDER BY sequence_no", "request-a");
        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).get("SEQUENCE_NO"));
        assertEquals(2, rows.get(1).get("SEQUENCE_NO"));
        assertEquals("getRuleHitsTool", rows.get(0).get("TOOL_NAME"));
        assertEquals("taskId=6", rows.get(0).get("INPUT_SUMMARY"));
        assertEquals("summary", rows.get(0).get("RESULT_SUMMARY"));
        assertEquals(true, rows.get(0).get("SUCCESS"));
        assertEquals(false, rows.get(1).get("SUCCESS"));
        assertNotNull(rows.get(0).get("CALLED_AT"));
    }

    @Test void traceCommitSurvivesOuterTransactionRollback() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            store.save("request-a", List.of(trace(true)));
            status.setRollbackOnly();
        });
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM agent_tool_trace", Integer.class));
    }

    @Test void failedBatchRollsBackAllItsRows() {
        var invalid = trace(false);
        invalid.setToolName(null);
        assertThrows(RuntimeException.class, () -> store.save("request-a", List.of(trace(true), invalid)));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM agent_tool_trace", Integer.class));
    }

    @Test void emptyCallsCreateNoRowsAndRepeatedSequenceIsRejected() {
        store.save("empty", List.of());
        store.save("request-a", List.of(trace(true)));
        assertThrows(RuntimeException.class, () -> store.save("request-a", List.of(trace(false))));
        store.save("request-b", List.of(trace(false)));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM agent_tool_trace", Integer.class));
    }
}
