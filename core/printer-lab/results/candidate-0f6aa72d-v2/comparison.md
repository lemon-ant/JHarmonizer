<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Printer comparison

Protocol, hardware identity, runtime, corpus and serialized output checks passed.

| Benchmark | Baseline | Candidate | Change % | Allocated B/batch change % | JMH mean CIs overlap |
| --- | ---: | ---: | ---: | ---: | --- |
| sample/fixtures/t1 | 1479.414 | 244.828 | -83.451 | -66.184 | false |
| sample/flat1024/t1 | 2029.643 | 1062.676 | -47.642 | -11.286 | false |
| sample/flat128/t1 | 238.747 | 139.615 | -41.522 | -13.947 | true |
| sample/nested128/t1 | 2592.803 | 1447.278 | -44.181 | -6.888 | false |
| thrpt/fixtures/t1 | 729.620 | 3552.142 | 386.848 | -66.007 | false |
| thrpt/fixtures/t2 | 1286.081 | 6522.571 | 407.167 | -65.809 | false |
| thrpt/fixtures/t4 | 1990.710 | 9911.235 | 397.874 | -65.858 | false |
| thrpt/fixtures/t8 | 2915.618 | 11873.051 | 307.222 | -65.850 | false |
| thrpt/flat1024/t1 | 497.883 | 988.897 | 98.620 | -11.259 | false |
| thrpt/flat1024/t2 | 944.433 | 1840.992 | 94.931 | -11.234 | false |
| thrpt/flat1024/t4 | 1444.030 | 2448.782 | 69.580 | -11.758 | false |
| thrpt/flat1024/t8 | 1740.199 | 2940.858 | 68.996 | -13.971 | false |
| thrpt/flat128/t1 | 4627.382 | 9749.091 | 110.683 | -14.414 | false |
| thrpt/flat128/t2 | 8344.599 | 14615.899 | 75.154 | -13.529 | false |
| thrpt/flat128/t4 | 12151.033 | 22424.573 | 84.549 | -13.532 | false |
| thrpt/flat128/t8 | 15721.098 | 27923.374 | 77.617 | -14.380 | false |
| thrpt/nested128/t1 | 362.436 | 765.913 | 111.324 | -6.712 | false |
| thrpt/nested128/t2 | 662.955 | 1276.957 | 92.616 | -7.449 | false |
| thrpt/nested128/t4 | 1220.759 | 1706.897 | 39.823 | -7.388 | true |
| thrpt/nested128/t8 | 1451.562 | 2286.030 | 57.488 | -7.519 | false |

Higher throughput is better; lower sampled time and allocation are better. CI overlap is descriptive and is not a significance test.

## Source metric changes

| Metric | Baseline | Candidate |
| --- | ---: | ---: |
| arity | {"max":6.0,"mean":1.6829268292682926,"p50":1.0,"p95":4.0} | {"max":5.0,"mean":1.7352941176470589,"p50":2.0,"p95":4.0} |
| astNodeInventory | {"Annotation":82,"AnnotationMemberList":4,"ArgumentList":255,"ArrayAccess":6,"ArrayDimensions":3,"ArrayType":3,"ArrayTypeDim":3,"AssignmentExpression":19,"Block":87,"BooleanLiteral":8,"BreakStatement":1,"CatchClause":1,"CatchParameter":1,"CharLiteral":11,"ClassBody":10,"ClassDeclaration":10,"ClassType":283,"ConditionalExpression":4,"ConstructorCall":13,"ConstructorDeclaration":5,"ContinueStatement":1,"ExplicitConstructorInvocation":2,"ExpressionStatement":62,"ExtendsList":2,"FieldAccess":18,"FieldDeclaration":19,"ForInit":4,"ForStatement":4,"ForUpdate":4,"ForeachStatement":2,"FormalParameter":69,"FormalParameters":41,"IfStatement":30,"ImportDeclaration":84,"InfixExpression":93,"LambdaExpression":24,"LambdaParameter":22,"LambdaParameterList":24,"LocalVariableDeclaration":59,"MemberValueArrayInitializer":1,"MemberValuePair":4,"MethodCall":240,"MethodDeclaration":36,"MethodReference":4,"ModifierList":232,"NullLiteral":6,"NumericLiteral":32,"PackageDeclaration":10,"PatternExpression":1,"PrimitiveType":79,"ReturnStatement":39,"StatementExpressionList":4,"StringLiteral":23,"SuperExpression":1,"ThisExpression":13,"ThrowStatement":3,"TryStatement":1,"TypeArguments":58,"TypeExpression":43,"TypeParameter":4,"TypeParameters":4,"TypePattern":1,"UnaryExpression":27,"VariableAccess":337,"VariableDeclarator":78,"VariableId":171,"VoidType":12,"WhileStatement":4,"WildcardType":22} | {"Annotation":61,"AnnotationMemberList":4,"ArgumentList":196,"ArrayAccess":2,"ArrayDimensions":3,"ArrayType":3,"ArrayTypeDim":3,"AssignmentExpression":15,"Block":63,"BooleanLiteral":6,"BreakStatement":1,"CatchClause":1,"CatchParameter":1,"CharLiteral":7,"ClassBody":10,"ClassDeclaration":10,"ClassType":186,"ConditionalExpression":6,"ConstructorCall":10,"ConstructorDeclaration":5,"ContinueStatement":1,"EnumBody":1,"EnumConstant":3,"EnumDeclaration":1,"ExpressionStatement":51,"FieldAccess":13,"FieldDeclaration":20,"ForInit":2,"ForStatement":2,"ForUpdate":2,"ForeachStatement":2,"FormalParameter":59,"FormalParameters":34,"IfStatement":20,"ImportDeclaration":53,"InfixExpression":83,"LambdaExpression":14,"LambdaParameter":14,"LambdaParameterList":14,"LocalVariableDeclaration":38,"MemberValueArrayInitializer":1,"MemberValuePair":4,"MethodCall":186,"MethodDeclaration":29,"MethodReference":4,"ModifierList":190,"NullLiteral":6,"NumericLiteral":30,"PackageDeclaration":9,"PatternExpression":1,"PrimitiveType":67,"ReturnStatement":29,"StatementExpressionList":2,"StringLiteral":15,"ThisExpression":7,"ThrowStatement":1,"TryStatement":1,"TypeArguments":31,"TypeExpression":26,"TypePattern":1,"UnaryExpression":21,"VariableAccess":275,"VariableDeclarator":58,"VariableId":136,"VoidType":10,"WhileStatement":2,"WildcardType":16} |
| cognitive | {"max":13.0,"mean":2.1219512195121952,"p50":1.0,"p95":9.0} | {"max":11.0,"mean":1.7352941176470589,"p50":0.0,"p95":9.0} |
| cognitiveOver15 | 0 | 0 |
| cyclo | {"max":8.0,"mean":2.5853658536585367,"p50":1.0,"p95":7.0} | {"max":9.0,"mean":2.3823529411764706,"p50":1.0,"p95":7.0} |
| cycloOver10 | 0 | 0 |
| declaredOperations | 41 | 34 |
| declaredTypesInWholeFiles | 10 | 11 |
| fanOut | {"max":22.0,"mean":5.146341463414634,"p50":4.0,"p95":13.0} | {"max":16.0,"mean":4.147058823529412,"p50":3.0,"p95":14.0} |
| loc | {"max":43.0,"mean":17.195121951219512,"p50":14.0,"p95":38.0} | {"max":32.0,"mean":13.205882352941176,"p50":11.0,"p95":29.0} |
| ncss | {"max":21.0,"mean":6.024390243902439,"p50":5.0,"p95":16.0} | {"max":19.0,"mean":5.264705882352941,"p50":3.0,"p95":16.0} |
| ncssSelected | 276 | 210 |
| npath | {"max":108.0,"mean":5.341463414634147,"p50":2.0,"p95":11.0} | {"max":42.0,"mean":3.9705882352941178,"p50":1.0,"p95":16.0} |
| physicalLinesSelected | 1000 | 701 |
| shortCircuitOperators | 24 | 21 |
| sourceFiles | 11 | 10 |
| sumCognitive | 87 | 59 |
| sumCyclo | 106 | 81 |

## Architecture changes

| Metric | Baseline | Candidate |
| --- | ---: | ---: |
| internalEdges | 15 | 18 |
| cycleCount | 0 | 1 |
| CCD | 30 | 39 |
| ACD | 2.727272727272727 | 3.25 |
| RACD | 0.24793388429752064 | 0.2708333333333333 |
| NCCD | 0.9090909090909091 | 1.054054054054054 |
| classes count | 11 | 12 |
| distinctExternalLibraryTargets count | 21 | 11 |
| CPD clone groups | 0 | 0 |
| PMD findings | 0 | 0 |
