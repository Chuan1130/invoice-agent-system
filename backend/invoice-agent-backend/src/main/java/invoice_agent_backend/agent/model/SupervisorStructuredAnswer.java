package invoice_agent_backend.agent.model;

import java.util.ArrayList;
import java.util.List;

/*
 ** Supervisor 的结构化输出。
 **
 ** 模型负责生成解释，但业务事实必须来自 Tool。
 ** 这里使用稳定字段，方便后续前端、评测和 Graph State 直接消费。
 */
public class SupervisorStructuredAnswer {

    private String intent;
    private String summary;
    private String riskLevel;
    private List<String> evidence;
    private String nextAction;

    public SupervisorStructuredAnswer() {
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<String> evidence) {
        this.evidence = evidence;
    }

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }

    public String toDisplayText() {

        List<String> safeEvidence =
                evidence == null
                        ? List.of()
                        : new ArrayList<>(evidence);

        StringBuilder builder = new StringBuilder();

        builder.append("【意图】\n")
                .append(valueOf(intent))
                .append("\n\n");

        builder.append("【结论】\n")
                .append(valueOf(summary))
                .append("\n\n");

        builder.append("【风险等级】\n")
                .append(valueOf(riskLevel))
                .append("\n\n");

        builder.append("【依据】\n");

        if (safeEvidence.isEmpty()) {
            builder.append("暂无可引用依据");
        } else {
            for (String item : safeEvidence) {
                builder.append("- ")
                        .append(valueOf(item))
                        .append("\n");
            }
        }

        builder.append("\n【下一步】\n")
                .append(valueOf(nextAction));

        return builder.toString().trim();
    }

    private String valueOf(String value) {
        return value == null || value.trim().isEmpty()
                ? "无"
                : value.trim();
    }
}
