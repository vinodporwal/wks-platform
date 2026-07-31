# AOP Approval Workflow (`AOP_Approval_v2`)

A 5-gate approve/revert workflow for the Annual Operating Plan, built on Camunda 7.
It is **fully isolated from the TCS workflow** (`TCS_APPROVAL_PROCESS`) — new process key,
new tables, new endpoints. No TCS class or table is modified.

---

## 1. The flow

```
Prepare (CTS Lead / Production Manager)
   │  submit
   ▼
Gate 1  Plant Manager ───────────────── R ──┐
   │ A                                      │
   ▼                                        │
Gate 2  Functional Heads (parallel) ─── R ──┤
   │ A   operation_head, technology_vertical_head,
   │     cts_head, fca_head, maintenance_head
   ▼                                        ├──► prepareRework ──► back to Gate 1
Gate 3  Site Head ───────────────────── R ──┤
   │ A                                      │
   ▼                                        │
Gate 4  GMS Business Head ───────────── R ──┘
   │ A
   ▼
Gate 5  GMS Head ───── R ──► Gate 3 (Site Head)
   │ A
   ▼
Approved Plan  →  Release (BPC / Board)
```

**Rules**

| Rule | Behaviour |
|---|---|
| Revert from Gates 1–4 | goes back to the **Preparer** (`prepareRework`), re-enters at Gate 1 |
| Revert from Gate 5 | goes back to **Gate 3 (Site Head)** |
| Multi-role gates | **every** gate supports 1..N approver roles (not just Gate 2) |
| Gate aggregation | **ALL must act, then aggregate** — any REVERTED ⇒ gate reverts; only all-approve advances. No early cancellation. |
| Identity | a workflow is keyed by **(plantId, aopYear)** — *not* the Camunda businessKey |
| Uniqueness | **at most one active workflow per plant+year** (service guard → 409, plus a filtered unique index) |

---

## 2. Configuration model — the only thing you configure

**`WorkflowStepRoles` is the single config surface.** Gates and routing are fixed in the BPMN;
the *approver roles per gate* are data.

```
WorkflowStepRoles: Id, WorkflowStep_FK_Id → WorkflowStepsMaster.Id, Role, isActive
```

* Add a role to any gate → **INSERT one row**
* Remove a role → set `isActive = 0`
* No BPMN change, no code change, no redeploy

Each gate is a **parallel multi-instance user task** over its active role set, so N=1 and N=5
behave identically. Site and vertical are **always resolved from the `Plants` master**
(`Plants.Site_FK_Id` / `Vertical_FK_Id`) — callers only ever pass `plantId` + `year`.

---

## 3. What was implemented

**BPMN** — `apps/java/services/demo-data-loader/data/camunda7/AOP_Approval_v2.bpmn`
Process key `AOP_Approval_v2`; gates as multi-instance user tasks over `${gateNRoles.split(',')}`;
exclusive gateways on `${gateNResult}`; includes BPMNDI so it renders in Cockpit.

**In-engine (runs inside the standalone Camunda server)**
`apps/java/libraries/c7-plugins/.../plugin/aop/AopGateDecisionListener.java`
Plain-Java `TaskListener` (`camunda:class`) — `create` resets the prior visit's result,
`complete` folds each approver's `decision` with REVERTED-wins. Uses only `RuntimeService`.

**Backend (`apps/java/libraries/case-engine` unless noted)**

| Concern | Class |
|---|---|
| Roles-per-gate config | `entity/WorkflowStepRoles`, `repository/WorkflowStepRolesRepository` |
| Audit trail | `entity/AopApprovalHistory`, `repository/AopApprovalHistoryRepository`, `service/AopApprovalAuditService(Impl)` |
| Orchestration (start / act) | `service/AopApprovalWorkflowService(Impl)` |
| Stage email | `service/AopWorkflowNotificationService` + `case-engine-rest-api/src/main/resources/templates/aop-workflow-template.html` |
| Uniqueness | `exception/WorkflowConflictException` (409) + guard in `service/WorkflowServiceImpl.submitWorkflow` |
| REST | `case-engine-rest-api/.../rest/server/AopApprovalController` |

**Frontend (`apps/react/case-portal`)**

* `src/services/AopApprovalService.js` — client for `/aop-approval/*`
* `src/components/data-tables/AOPWorkFlow/kendo-WorkFlowMerge.js` — Approve / Revert / Submit driven by the server `viewer`
* `src/components/data-tables/AOPWorkFlow/AopMyApprovals.js` — "My Approvals" inbox (route `aop-approvals`)

**Audit row captures:** caseId, year, plant/site/vertical (**ids + names**), gate, sequence,
action (`SUBMITTED|APPROVED|REVERTED`), actorUserId, **actorRole**, remark, from/toGate, actionAt.

---

## 4. Deploy — required steps in order

> ⚠️ **Step 1 is not optional.** `apps/java/services/case-engine-rest-api/Dockerfile` does
> `COPY target/*.jar app.jar` — it **does not build from source**. If you skip the Maven build,
> Docker will happily ship a stale jar and none of this workflow will exist at runtime.

### 1. Build the Java artifacts (JDK 17)

```bash
export JAVA_HOME=/path/to/jdk-17          # project targets 17; Lombok 1.18.22 breaks on JDK 21+
cd apps/java

# API jar (the one the Dockerfile copies)
mvn -pl services/case-engine-rest-api -am package -DskipTests

# c7-plugins jar — mounted into Camunda's userlib (gate decision listener)
mvn -pl libraries/c7-plugins -am package -DskipTests
```

Produces:
* `apps/java/services/case-engine-rest-api/target/case-engine-rest-api-1.0-SNAPSHOT.jar`
* `apps/java/libraries/c7-plugins/target/c7-plugins-1.0-SNAPSHOT.jar` ← mounted by
  `docker-compose.event-hub.camunda7.yaml` into `/camunda/configuration/userlib/`

### 2. Environment

```bash
cp .env.example .env     # then fill the TODOs
```
`CAMUNDA_DOCKER_IMAGE`/`CAMUNDA7_VERSION` **must be the Camunda _Run_ distribution**
(it uses `/camunda/configuration/userlib/`). Verify nothing is unresolved:

```bash
docker-compose -f docker-compose.yaml -f docker-compose.camunda7.yaml ... config | grep "variable is not set"
```

### 3. Start the stack

```bash
./scripts/linux/docker-full-startup.sh     # backend + portal
./scripts/linux/docker-portal-startup.sh   # portal only
```

The `demo-data-loader` auto-deploys every `.bpmn` in
`apps/java/services/demo-data-loader/data/camunda7/` — including `AOP_Approval_v2.bpmn`.

To redeploy the BPMN manually without restarting the loader:

```bash
curl -X POST http://localhost:8080/engine-rest/deployment/create \
  -F "deployment-name=aop-approval-v2" -F "tenant-id=localhost" \
  -F "AOP_Approval_v2.bpmn=@apps/java/services/demo-data-loader/data/camunda7/AOP_Approval_v2.bpmn"
```

### 4. Keycloak roles — **required**

Create these **11 realm roles** (the workflow assigns tasks to role names):

```
cts_lead  production_manager  plant_manager
operation_head  technology_vertical_head  cts_head  fca_head  maintenance_head
site_head  gms_business_head  gms_head
```

Scripted (adjust realm / URL):

```bash
REALM=localhost; KC=http://localhost:8082
TOK=$(curl -s -d client_id=admin-cli -d username=$KC_ADMIN -d password=$KC_PW \
      -d grant_type=password $KC/realms/master/protocol/openid-connect/token \
      | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
for r in cts_lead production_manager plant_manager operation_head \
         technology_vertical_head cts_head fca_head maintenance_head \
         site_head gms_business_head gms_head; do
  curl -s -X POST -H "Authorization: Bearer $TOK" -H "Content-Type: application/json" \
    -d "{\"name\":\"$r\"}" "$KC/admin/realms/$REALM/roles"
done
```

Then **assign roles to users** and ensure each user has an **email address** (recipients are
resolved role → Keycloak users → email).

### 5. SQL scripts — **required**

Run against the **AOP SQL Server database** (`RIL.AOP`, the `db1`/`SQL_SERVER_*` datasource):

| Script | Purpose | Before running |
|---|---|---|
| `sql/AOP_Approval_Workflow_Seed.sql` | Seeds `WorkflowMaster`, the 6 `WorkflowStepsMaster` gates (`prepare`, `gate1..gate5`) and `WorkflowStepRoles` (Gate 2 → 5 rows, others → 1) | **Set `@VerticalName`** to the AOP vertical's `Name` in `dbo.Verticals`. Optionally `@WorkflowId`/`@CaseDefId` (default `AOP_Approval_v2`). |
| `sql/AOP_Approval_Workflow_UniqueIndex.sql` | Filtered unique index on `WorkflowInstances(Plant_FK_Id, Year) WHERE isDeleted = 0` | none |

Both are **idempotent** — safe to re-run.

```bash
sqlcmd -S <host> -U <user> -P <pw> -d "RIL.AOP" -i sql/AOP_Approval_Workflow_Seed.sql
sqlcmd -S <host> -U <user> -P <pw> -d "RIL.AOP" -i sql/AOP_Approval_Workflow_UniqueIndex.sql
```

> The tables themselves (`WorkflowStepRoles`, `AOP_Approval_History`) are created automatically
> by Hibernate (`spring.jpa.hibernate.ddl-auto: update`) on first API start. Start the API
> **before** running the seed.

### 6. Email (optional)

Set `spring.mail.*` (`SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `spring.mail.fromEmail`, …).
If mail is **not** configured the API still starts and approvals still work — notifications
log a warning and are skipped.

---

## 5. API

Base path `/aop-approval` — all endpoints take identity from the JWT.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/aop-approval/start?plantId=&year=` | Start a workflow (409 if one is already active) |
| `POST` | `/aop-approval/act` | Apply a gate decision — body: `{taskId, plantId, year, gateName, decision, remark, actorRole}`, `decision = APPROVED\|REVERTED` |
| `GET` | `/aop-approval/status?plantId=&year=` | Stepper + current gate + **`viewer`** button state |
| `GET` | `/aop-approval/my-pending` | The caller's inbox across all plants |
| `GET` | `/aop-approval/audit-trail?plantId=&year=` | Full audit trail |

**`viewer` drives the UI — the client never decides actionability:**

```jsonc
"viewer": {
  "mode": "ACTION | READ_ONLY | EDIT",
  "canApprove": true, "canRevert": true,
  "canSubmit": false, "canEdit": false,
  "remarkMandatory": true          // from WorkflowStepsMaster.isRemarksDisabled (inverted)
}
```
`mode = ACTION` only when an active task's assignee-role ∈ the caller's JWT roles.

---

## 6. Verify

```bash
# BPMN deployed (expect key AOP_Approval_v2)
curl -s "http://localhost:8080/engine-rest/process-definition?key=AOP_Approval_v2"

# API up — 401 is CORRECT without a token (endpoint exists, security enforced)
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8081/aop-approval/my-pending

# Tables created
sqlcmd ... -Q "SELECT name FROM sys.tables WHERE name IN ('WorkflowStepRoles','AOP_Approval_History')"

# Gate roles seeded
sqlcmd ... -Q "SELECT s.Name, r.Role FROM WorkflowStepRoles r
               JOIN WorkflowStepsMaster s ON s.Id = r.WorkflowStep_FK_Id
               WHERE r.isActive = 1 ORDER BY s.Sequence"
```

**End-to-end:** start a plan → approve through Gates 1→5 → confirm `Approved`; then repeat and
(a) revert at Gate 2 (verify it lands on the preparer), (b) revert at Gate 5 (verify it lands on
Gate 3). Check `AOP_Approval_History` has a row per action with the correct `actorRole`.

---

## 7. Gotchas (learned the hard way)

| Symptom | Cause / fix |
|---|---|
| New code isn't running; odd beans (e.g. `ldapConfig`) fail | **Stale jar.** The API Dockerfile copies `target/*.jar`. Run the Maven build (§4.1) before `docker-compose build`. |
| Compile fails: `X is not abstract and does not override…` | **JDK 21+ with Lombok 1.18.22.** Build with **JDK 17**. |
| Process deployed but blank in Cockpit | BPMN had no BPMNDI. This file now includes it — the `"diagram":null` REST field is a separate image resource and is unrelated. |
| Gate listener never fires | `c7-plugins` jar missing/stale — Camunda mounts `libraries/c7-plugins/target/c7-plugins-1.0-SNAPSHOT.jar` into `userlib/`. Rebuild it. |
| `yarn install --frozen-lockfile` fails | `yarn.lock` out of sync with `package.json` — run `yarn install` to update the lockfile, commit it. |
| Postgres exits (3) on first boot | `init.sql` runs `ALTER TABLE user_attribute …` before Keycloak creates its schema. Restart postgres — init scripts are skipped once the data dir exists. |
| `bind: address already in use` on 5432 | A host Postgres owns 5432. Remap the published port (Keycloak reaches it internally at `postgres:5432`). |
| `docker-credential-desktop: not found` | Add Docker.app's bin to PATH: `export PATH="/Applications/Docker.app/Contents/Resources/bin:$PATH"`. |

---

## 8. Extending

* **Add/remove an approver role on any gate** → `WorkflowStepRoles` row (`isActive`). Nothing else.
* **Change a gate's remark requirement** → `WorkflowStepsMaster.isRemarksDisabled`.
* **Change gates or routing** → BPMN edit + redeploy (gates/routing are intentionally fixed).
