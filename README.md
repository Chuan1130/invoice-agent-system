# Intelligent Invoice Reimbursement Audit Multi-Agent System

An intelligent invoice reimbursement auditing system designed to automate invoice processing, risk detection, audit decision-making, human review, and report generation.

The project is being developed incrementally:

**Business Backend → Real OCR → Supervisor Agent + Tools → Multi-Agent Graph → Frontend Integration**

The current version has completed the core backend audit workflow and provides the business foundation for future AI Agent integration.

---

## 1. Project Overview

Traditional invoice systems mainly focus on uploading, storing, and querying invoice records.

This project extends that process into a complete reimbursement audit workflow:

```text
Invoice Upload
      ↓
Create Audit Task
      ↓
OCR Recognition
      ↓
Structured Invoice Data
      ↓
Audit Rules
      ↓
Risk Detection
      ↓
Generate Audit Report
      ↓
Automatic Approval
      OR
Human Review
      ↓
Final Decision
```

The final goal is to develop this workflow into an AI-driven Multi-Agent system in which different Agents are responsible for OCR, policy checking, historical data querying, risk analysis, and report generation.

---

## 2. Current System Architecture

```text
                    Client / Frontend
                          │
                          ▼
                   REST API Layer
                          │
                          ▼
                 Spring Boot Backend
                          │
                          ▼
                AuditTaskService
                          │
                          ▼
          InvoiceAuditWorkflowService
                          │
                          ▼
                  AuditTaskState
                          │
        ┌─────────────────┼─────────────────┐
        ▼                 ▼                 ▼
   File Storage       MySQL Database     Business Services
                                              │
                           ┌──────────────────┼──────────────────┐
                           ▼                  ▼                  ▼
                      OCR Service       Audit Rules       Report Service
                           │
                           ▼
                     InvoiceInfo
                           │
                           ▼
                     Audit Result
                           │
                  ┌────────┴────────┐
                  ▼                 ▼
              APPROVED      NEED_HUMAN_REVIEW
                                    │
                                    ▼
                              Human Review
                               ┌─────┴─────┐
                               ▼           ▼
                           APPROVE       REJECT
                               │           │
                               └─────┬─────┘
                                     ▼
                                 COMPLETED
```

---

## 3. Current Workflow

The current backend workflow is explicitly divided into several processing nodes:

```text
validateUpload
      ↓
saveInvoiceFile
      ↓
createAuditTask
      ↓
executeOcr
      ↓
executeAuditRules
      ↓
generateAuditReport
      ↓
buildResult
```

`AuditTaskState` is used to carry workflow state between these processing steps.

This structure is intentionally designed so that the current workflow can later evolve into an Agent Graph.

---

## 4. Implemented Features

### Backend Infrastructure

* Spring Boot REST API
* MyBatis persistence layer
* MySQL integration
* Unified API response structure
* Global exception handling
* Multipart file upload
* Transaction management
* Maven project configuration
* Health check endpoint

### Invoice Upload

* Multipart invoice upload
* Empty file validation
* UUID-based file naming
* Date-based file directories
* Local invoice file storage
* Maximum upload size: 10 MB

Uploaded files are stored under:

```text
uploads/invoices/yyyyMMdd/
```

### Audit Task Management

Each uploaded invoice creates an independent audit task.

The audit task records:

* Task ID
* Task number
* Current status
* Original invoice path
* OCR raw result
* Final audit decision
* Human review requirement
* Audit report path
* Creation and update time

### OCR

The system defines an abstract:

```text
OcrService
```

The current implementation is:

```text
MockOcrServiceImpl
```

It returns predefined invoice information for workflow development and testing.

The interface design allows a real OCR provider to replace the Mock implementation without changing the rest of the audit workflow.

### Structured Invoice Data

OCR results are converted into structured invoice information containing:

* Invoice number
* Invoice date
* Amount
* Tax amount
* Buyer name
* Seller name
* Invoice type
* Raw OCR JSON

The data is persisted in MySQL.

### Audit Rules

Three basic audit rules are currently implemented.

#### 1. Required Field Check

Checks whether the following fields are missing:

* Invoice number
* Buyer name
* Seller name

Rule code:

```text
RULE_REQUIRED_FIELD
```

#### 2. Amount Limit Check

Invoices with an amount greater than:

```text
50,000
```

are marked for human review.

Rule code:

```text
RULE_AMOUNT_LIMIT
```

#### 3. Duplicate Invoice Check

The system checks historical invoice records for an existing invoice with the same invoice number.

Rule code:

```text
RULE_DUPLICATE_INVOICE
```

### Audit Decision

If no rule is triggered:

```text
APPROVED
```

If at least one rule is triggered:

```text
NEED_HUMAN_REVIEW
```

Each triggered rule is independently stored so the system can explain why a task requires manual review.

### Audit Reports

The system generates a TXT audit report containing:

* Task information
* OCR result
* Invoice information
* Triggered audit rules
* Final decision
* System recommendation

Reports are stored under:

```text
uploads/reports/yyyyMMdd/
```

Example:

```text
AUDIT_REPORT_12.txt
```

The report path is also stored in the database.

### Human-in-the-loop Review

Tasks requiring human review can be manually:

```text
APPROVE
```

or:

```text
REJECT
```

The system records:

* Reviewer
* Review decision
* Review comment
* Review time

The final decision becomes:

```text
APPROVED_BY_HUMAN
```

or:

```text
REJECTED_BY_HUMAN
```

The task is then updated to:

```text
COMPLETED
```

After human review, the audit report is regenerated using the final human decision.

### Audit Task Query

The backend currently supports:

* Task pagination
* Status filtering
* Single task query
* OCR information query
* Rule hit query
* Audit report query
* Aggregated task detail query
* Human review history

---

## 5. REST API

### Health Check

```http
GET /health
```

### Upload Invoice and Execute Audit Workflow

```http
POST /api/invoices/upload
```

Request:

```text
multipart/form-data
file = invoice image
```

### Audit Task List

```http
GET /api/invoices/tasks
```

Example:

```http
GET /api/invoices/tasks?page=1&size=20
```

Status filtering:

```http
GET /api/invoices/tasks?status=AUDIT_DONE&page=1&size=20
```

### Audit Task

```http
GET /api/invoices/tasks/{id}
```

### OCR / Invoice Information

```http
GET /api/invoices/tasks/{id}/invoice-info
```

### Audit Rule Hits

```http
GET /api/invoices/tasks/{id}/rule-hits
```

### Audit Report

```http
GET /api/invoices/tasks/{id}/report
```

### Complete Task Detail

```http
GET /api/invoices/tasks/{id}/detail
```

This endpoint aggregates:

```text
AuditTask
+
InvoiceInfo
+
AuditRuleHits
+
AuditReport
+
HumanReviewRecords
```

### Human Review

```http
POST /api/invoices/tasks/{id}/human-review
```

Example:

```json
{
  "decision": "APPROVE",
  "reviewer": "Reviewer Name",
  "comment": "Invoice manually checked and approved."
}
```

Supported decisions:

```text
APPROVE
REJECT
```

---

## 6. Task Status

Current task states:

```text
UPLOADED
    ↓
OCR_DONE
    ↓
AUDIT_DONE
    ↓
Human Review if required
    ↓
COMPLETED
```

Current audit decisions:

```text
APPROVED

NEED_HUMAN_REVIEW

APPROVED_BY_HUMAN

REJECTED_BY_HUMAN
```

---

## 7. Database Model

The current backend mainly uses four business tables:

```text
audit_task
    │
    ├── invoice_info
    │
    ├── audit_rule_hit
    │
    └── human_review_record
```

### `audit_task`

Stores the main audit workflow state.

### `invoice_info`

Stores structured invoice information generated by OCR.

### `audit_rule_hit`

Stores each audit rule triggered by a task.

### `human_review_record`

Stores human review history.

`task_id` connects the records across the entire audit workflow.

---

## 8. Technology Stack

### Current Backend

* Java 17
* Spring Boot 3.5
* Spring MVC
* MyBatis
* MySQL
* Maven
* Maven Wrapper
* Spring Validation

### Prepared / Planned Technologies

* Redis
* Real OCR API
* Spring AI
* Spring AI Alibaba Graph
* LLM Tool Calling
* Structured Output
* Vue / React frontend

Redis dependencies and configuration currently exist, but Redis is not yet used by the main business workflow.

---

## 9. Project Structure

```text
invoice-agent-system/
│
├── README.md
│
├── backend/
│   └── invoice-agent-backend/
│       ├── pom.xml
│       ├── mvnw
│       ├── mvnw.cmd
│       └── src/
│           ├── main/
│           │   ├── java/
│           │   │   └── invoice_agent_backend/
│           │   │       ├── common/
│           │   │       ├── constant/
│           │   │       ├── controller/
│           │   │       ├── dto/
│           │   │       ├── entity/
│           │   │       ├── exception/
│           │   │       ├── mapper/
│           │   │       ├── service/
│           │   │       ├── vo/
│           │   │       └── workflow/
│           │   └── resources/
│           │       └── mapper/
│           └── test/
│
└── frontend/
    └── Planned / under development
```

The frontend directory shown above represents the planned project-level structure and may not yet contain an implemented frontend.

---

## 10. Running the Backend

### Requirements

Install:

* JDK 17
* MySQL
* Git

Redis may also be installed for future development, but the current core workflow does not depend on Redis business logic.

### Clone the Repository

```bash
git clone <repository-url>
cd invoice-agent-system
```

### Enter the Backend

```bash
cd backend/invoice-agent-backend
```

### Configure MySQL

Create or prepare:

```text
Database: invoice_agent
```

The current local configuration uses:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/invoice_agent
spring.datasource.username=root
```

Database credentials should be adjusted according to the local development environment.

The required business tables must already exist before running the current version.

### Compile

Windows:

```bash
.\mvnw.cmd clean compile
```

### Run Tests

```bash
.\mvnw.cmd test
```

### Start Backend

```bash
.\mvnw.cmd spring-boot:run
```

The backend runs at:

```text
http://localhost:8080
```

Check:

```text
http://localhost:8080/health
```

Expected response:

```text
ok
```

---

## 11. Current Development Status

The current implementation should be considered:

> **The business backend MVP and workflow foundation of the Multi-Agent invoice audit system.**

The following components are already implemented:

```text
Upload
   ↓
Audit Task
   ↓
Mock OCR
   ↓
Structured Data
   ↓
Audit Rules
   ↓
Risk Detection
   ↓
Report
   ↓
Task Query
   ↓
Human Review
   ↓
Final Decision
```

However, the project is **not yet a complete Multi-Agent system**.

---

## 12. Current Limitations

The following components have not yet been fully implemented:

* Real OCR recognition
* OCR confidence handling
* LLM integration
* Tool Calling
* Structured LLM Output
* Supervisor Agent
* Multi-Agent runtime
* Spring AI Alibaba Graph
* Agent conditional routing
* Agent execution trace
* Redis-backed workflow state
* Authentication and authorization
* Frontend application
* PDF audit reports
* Database migration scripts
* Comprehensive automated tests
* Workflow failure recovery
* File compensation mechanism
* Production deployment

---

## 13. Planned Agent Architecture

The planned architecture is:

```text
                     Supervisor Agent
                           │
        ┌──────────────────┼──────────────────┐
        ▼                  ▼                  ▼
    OCR Agent         Policy Agent        Data Agent
        │                  │                  │
        └──────────────────┼──────────────────┘
                           ▼
                       Risk Agent
                           │
                           ▼
                      Report Agent
                           │
                 ┌─────────┴─────────┐
                 ▼                   ▼
          Automatic Decision     Human Review
```

Planned responsibilities:

### Supervisor Agent

* Understand task requirements
* Route workflow
* Select tools / Agents
* Aggregate results
* Decide the next processing step

### OCR Agent

* Recognize invoices
* Extract structured fields
* Handle OCR confidence and missing data

### Policy Agent

* Query reimbursement policies
* Check limits and policy requirements

### Data Agent

* Query historical invoice records
* Detect duplicates
* Retrieve historical reimbursement information

### Risk Agent

* Combine OCR, rules, policy, and historical information
* Produce risk assessment

### Report Agent

* Generate human-readable audit reports
* Summarize audit evidence and recommendations

---

## 14. Development Roadmap

### Phase 1 — Business Backend

* [x] Spring Boot backend
* [x] File upload
* [x] Audit task model
* [x] OCR abstraction
* [x] Mock OCR
* [x] Rule audit
* [x] Duplicate invoice detection
* [x] Audit report
* [x] Task pagination
* [x] Task detail
* [x] Human review
* [x] Explicit workflow
* [ ] Complete automated regression tests
* [ ] Workflow failure handling

### Phase 2 — Real OCR

* [ ] Integrate real OCR provider
* [ ] OCR response mapping
* [ ] OCR error handling
* [ ] OCR confidence handling
* [ ] OCR retry mechanism

### Phase 3 — Supervisor + Tools

* [ ] Integrate LLM
* [ ] Structured Output
* [ ] Tool Calling
* [ ] OCR Tool
* [ ] Audit Rule Tool
* [ ] Historical Data Tool
* [ ] Report Tool
* [ ] Supervisor Agent

### Phase 4 — Multi-Agent Graph

* [ ] Spring AI Alibaba Graph
* [ ] OCR Agent
* [ ] Policy Agent
* [ ] Data Agent
* [ ] Risk Agent
* [ ] Report Agent
* [ ] Conditional edges
* [ ] Retry / recovery
* [ ] Agent trace
* [ ] Human-in-the-loop Graph node

### Phase 5 — Productization

* [ ] Frontend
* [ ] Invoice upload page
* [ ] Audit task dashboard
* [ ] Audit task detail page
* [ ] Human review interface
* [ ] Workflow visualization
* [ ] Agent execution visualization
* [ ] Authentication
* [ ] PDF export
* [ ] Docker deployment
* [ ] CI/CD

---

## 15. Project Direction

The project follows one core development principle:

```text
Build reliable business capabilities first
        ↓
Expose them as reusable Tools
        ↓
Introduce one Supervisor Agent
        ↓
Upgrade to a Multi-Agent Graph
```

This avoids building an Agent system without real business capabilities underneath it.

The existing Spring Boot services and workflow are therefore not temporary code that will be discarded later.

They are intended to become the deterministic business tools used by the future AI Agents.
