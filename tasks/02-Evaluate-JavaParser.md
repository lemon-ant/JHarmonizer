
## Task 2 – Evaluate JavaParser on Complex Java Source

**Objective:**  
Use a real or synthetic Java source file that includes a variety of language features (e.g., comments, annotations, nested types, initializers) to perform a practical evaluation of JavaParser.

If the full test project (from Task 1) is already available, reuse it. Otherwise, prepare a smaller sample manually with the following elements:
- Class-level and field-level annotations
- Nested classes or interfaces
- Different types of comments (Javadoc, single-line, multi-line, floating)
- Lambda and stream expressions
- Fields depending on each other
- Static and instance initializers

**What to do:**
1. Parse the code using JavaParser.
2. Modify the order of class members.
3. Serialize the AST back into source code.
4. Verify that the output is structurally and syntactically valid.
5. Document your findings.

**Evaluation Criteria:**  
Perform analysis according to the following aspects:

| Criterion                                 | JavaParser |
|-------------------------------------------|------------|
| Support for latest Java versions          |            |
| AST quality                               |            |
| Documentation level                       |            |
| Integration complexity                    |            |
| Code re-serialization (pretty-printing)   |            |
| Annotation processing                     |            |
| Nested class handling                     |            |
| Dependency tracking (e.g. between fields) |            |
| AST API flexibility                       |            |

Results should be recorded in a structured format, e.g. table or bullet points.  
No need to draw final conclusions at this stage — focus on data collection.
