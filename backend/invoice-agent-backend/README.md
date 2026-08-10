# Intelligent Invoice Audit Multi-Agent System

An intelligent invoice reimbursement audit backend built with Spring Boot, MyBatis and MySQL.

The current version implements the core invoice audit workflow and provides the business foundation for future Supervisor Agent and Multi-Agent Graph integration.

## Current Workflow

Invoice Upload
→ Create Audit Task
→ OCR Recognition
→ Audit Rules
→ Generate Audit Report
→ Human Review if Required
→ Final Decision

## Implemented Features

* Invoice file upload
* Audit task creation and status tracking
* Mock OCR service
* Structured invoice information storage
* Required field validation
* Invoice amount limit checking
* Duplicate invoice detection
* Audit rule hit records
* Automatic audit decision
* TXT audit report generation
* Audit task pagination and filtering
* Full audit task detail query
* Human-in-the-loop review
* Manual approve / reject
* Final report regeneration after human review
* Unified API response
* Global exception handling
* Workflow-based audit orchestration

## Current Audit Rules

1. Required Field Check

    * Invoice number
    * Buyer name
    * Seller name

2. Amount Limit Check

    * Amount greater than 50,000 requires human review

3. Duplicate Invoice Check

    * Detects duplicate invoice numbers in historical records

## Technology Stack

* Java 17
* Spring Boot 3.5
* Spring MVC
* MyBatis
* MySQL
* Maven
* Redis dependency prepared for future use

## Main API

```text
GET  /health

POST /api/invoices/upload

GET  /api/invoices/tasks
GET  /api/invoices/tasks/{id}
GET  /api/invoices/tasks/{id}/invoice-info
GET  /api/invoices/tasks/{id}/rule-hits
GET  /api/invoices/tasks/{id}/report
GET  /api/invoices/tasks/{id}/detail

POST /api/invoices/tasks/{id}/human-review
```

## Task Status

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

## Audit Decision

```text
APPROVED

NEED_HUMAN_REVIEW

APPROVED_BY_HUMAN

REJECTED_BY_HUMAN
```

## Current Limitations

* OCR currently uses MockOcrServiceImpl
* Real OCR provider has not yet been integrated
* LLM and Tool Calling have not yet been integrated
* Supervisor Agent has not yet been implemented
* Multi-Agent Graph has not yet been implemented
* Redis is configured but not yet used by business logic
* Frontend integration is still pending

## Next Steps

1. Complete full API regression testing
2. Improve workflow error handling and task status management
3. Integrate real OCR
4. Encapsulate existing business services as Agent Tools
5. Implement Supervisor Agent
6. Upgrade workflow to Multi-Agent Graph
7. Integrate frontend and audit workflow visualization
