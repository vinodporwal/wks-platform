# Concise API Generation Prompt

You can copy-paste the compact prompt below into any LLM (e.g. Gemini, ChatGPT) to generate new APIs matching the architectural pattern of the `imported-power-plans` API in this codebase.

---

```markdown
Act as an expert Spring Boot developer. Generate a complete backend API suite (REST Controller, Service interface, Service implementation, Entity, JPA Repository, and DTOs) for a new feature: [INSERT FEATURE NAME AND FIELDS HERE]

Follow these exact architectural rules and coding standards:

1. **REST Controller**: Mapped under `@RequestMapping("/task")`. Injects service interface, logs all incoming/outgoing requests, and returns `AOPMessageVM` response wrappers (containing int `code`, String `message`, Object `data`).
2. **Service & ServiceImpl**: Interface and implementation (annotated with `@Service`, and `@Transactional` on writing methods). Incorporate validation logic and SHA-256 hash-based change detection (e.g. for batch saves) to skip DB updates for unchanged rows.
3. **Data Access**: Repository extending `JpaRepository<Entity, UUID>` using Spring Data JPA.
4. **DTOs**: Created under `com.wks.caseengine.dto` using Lombok `@Data` annotations.
5. **Excel Integration**: Use Apache POI (`XSSFWorkbook`) to export records as styled spreadsheets (bold headers, auto-fit columns). For imports, parse files, run checks, save valid rows, and return a base64-encoded error Excel workbook listing validation failures on invalid rows.
```
