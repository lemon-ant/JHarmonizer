## 📌 Problem Statement

JHarmonizer must implement an efficient and extensible internal architecture for processing `.java` files.
The tool supports three primary workflows:

1. **Restructure** — reorganize code and write changes to disk.
2. **Check (fail-fast)** — validate structure, stop on the first mismatch.
3. **Check (collect)** — validate all files, collect mismatches and statistics.

File traversal must be **recursive and parallel**. All stages (parse, sort, format, compare, write) should emit **stage-wise statistics**, be **deterministic, loggable, and profiled**.

---

## 🔁 Architectural Options

### 🅰️ Option A: Stream-based pipeline

Each file is processed in `processFile(...)`, implemented as an internal stream of `.map()` / `.filter()` / `.peek()` stages passing a `FileContext` object.

**Pros:**
- High modularity.
- Functional style.
- Easy to unit test individual stages.

**Cons:**
- Stats collection is cumbersome (state must be threaded through the pipeline).
- Debugging and logging are nontrivial.
- Error handling lacks context.
- Increased GC pressure due to intermediate objects.

---

### 🅱️ Option B: Procedural `WorkflowRunner`

The function `processFile(path)` invokes `WorkflowRunner.run(path)`, which runs all processing steps sequentially (`parse`, `sort`, `format`, `diff`, `write`, etc.).
Intermediate results are stored in local variables.

**Pros:**
- Minimal allocations, optimal GC behavior.
- Clear and debuggable step-by-step logic.
- Simple and robust logging.
- Straightforward and accurate stats collection.
- High performance under load.

**Cons:**
- Slightly more boilerplate.
- Testing requires extracting steps into methods.

---

## ✅ Recommended Hybrid Strategy

Use a **hybrid architecture**:

- **Outside**: `Files.walk(...).parallel().map(path -> WorkflowRunner.run(path))`
- **Inside**: `WorkflowRunner` performs **procedural** file processing, gathering all metrics and diagnostics.

This offers:
- 🔥 **Maximum performance**
- 🧠 **Full control over logic and error paths**
- 🧰 **Extensibility for logging, reporting, and CI integration**
- 📈 **Reliable and reproducible statistics**

---

## 📊 Final Comparison Table

| Criteria                  | Stream Pipeline | Procedural `WorkflowRunner` |
|---------------------------|------------------|-------------------------------|
| Raw performance           | ❌               | ✅                            |
| Parallel execution        | ✅ (external)     | ✅ (external)                  |
| Logging                   | ⚠️ Complex        | ✅ Straightforward             |
| Debugging                 | ⚠️ Limited        | ✅ IDE-friendly                |
| Error handling            | ⚠️ Fragile        | ✅ Fully controlled            |
| Stats aggregation         | ⚠️ Tedious        | ✅ Local and explicit          |
| Workflow extensibility    | ⚠️ Rigid          | ✅ Flexible                    |

---

## 🧩 Next Steps

1. Implement `WorkflowRunner.run(Path path)` as the per-file processor.
2. Define `FileProcessingReport` and `StepStats` data models.
3. Use `parallelStream().map(...)` to invoke runner and aggregate into a global `ProcessingReport`.
