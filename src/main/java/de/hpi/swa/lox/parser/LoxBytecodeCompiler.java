package de.hpi.swa.lox.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.bytecode.BytecodeLocal;
import com.oracle.truffle.api.bytecode.BytecodeParser;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;

import de.hpi.swa.lox.LoxLanguage;
import de.hpi.swa.lox.bytecode.LoxBytecodeRootNodeGen;
import de.hpi.swa.lox.nodes.LoxRootNode;
import de.hpi.swa.lox.parser.LoxParser.ArgumentsContext;
import de.hpi.swa.lox.parser.LoxParser.ArrayAssignmentContext;
import de.hpi.swa.lox.parser.LoxParser.ArrayContext;
import de.hpi.swa.lox.parser.LoxParser.ArrayExprContext;
import de.hpi.swa.lox.parser.LoxParser.AssignmentContext;
import de.hpi.swa.lox.parser.LoxParser.BlockContext;
import de.hpi.swa.lox.parser.LoxParser.BooleanContext;
import de.hpi.swa.lox.parser.LoxParser.CallArgumentsContext;
import de.hpi.swa.lox.parser.LoxParser.CallContext;
import de.hpi.swa.lox.parser.LoxParser.ComparisonContext;
import de.hpi.swa.lox.parser.LoxParser.EqualityContext;
import de.hpi.swa.lox.parser.LoxParser.ExprStmtContext;
import de.hpi.swa.lox.parser.LoxParser.ExpressionContext;
import de.hpi.swa.lox.parser.LoxParser.FactorContext;
import de.hpi.swa.lox.parser.LoxParser.FalseContext;
import de.hpi.swa.lox.parser.LoxParser.ForInStmtContext;
import de.hpi.swa.lox.parser.LoxParser.ForOfStmtContext;
import de.hpi.swa.lox.parser.LoxParser.ForStmtContext;
import de.hpi.swa.lox.parser.LoxParser.FunDeclStmtContext;
import de.hpi.swa.lox.parser.LoxParser.FunctionContext;
import de.hpi.swa.lox.parser.LoxParser.IfStmtContext;
import de.hpi.swa.lox.parser.LoxParser.Logic_andContext;
import de.hpi.swa.lox.parser.LoxParser.Logic_orContext;
import de.hpi.swa.lox.parser.LoxParser.NilContext;
import de.hpi.swa.lox.parser.LoxParser.NumberContext;
import de.hpi.swa.lox.parser.LoxParser.ParametersContext;
import de.hpi.swa.lox.parser.LoxParser.PrimaryContext;
import de.hpi.swa.lox.parser.LoxParser.PrintStmtContext;
import de.hpi.swa.lox.parser.LoxParser.ProgramContext;
import de.hpi.swa.lox.parser.LoxParser.ReturnStmtContext;
import de.hpi.swa.lox.parser.LoxParser.StringContext;
import de.hpi.swa.lox.parser.LoxParser.TermContext;
import de.hpi.swa.lox.parser.LoxParser.TrueContext;
import de.hpi.swa.lox.parser.LoxParser.UnaryContext;
import de.hpi.swa.lox.parser.LoxParser.VarDeclContext;
import de.hpi.swa.lox.parser.LoxParser.VariableExprContext;
import de.hpi.swa.lox.parser.LoxParser.WhileStmtContext;
import de.hpi.swa.lox.runtime.LoxContext;
import de.hpi.swa.lox.runtime.data.LoxNumber;
import de.hpi.swa.lox.runtime.data.Nil;

/**
 * Lox AST visitor that parses to Bytecode DSL bytecode.
 */
public final class LoxBytecodeCompiler extends LoxBaseVisitor<Void> {

    protected final LoxLanguage language;
    protected final Source source;

    private final LoxBytecodeRootNodeGen.Builder b;

    private LoxLexicalScope lexicalScope = new LoxLexicalScope();

    /**
     * Inner class for variable scoping.
     */
    private class LoxLexicalScope {

        /**
         * Used for stacking variable scopes.
         */
        private final LoxLexicalScope parentScope;

        private final Map<String, BytecodeLocal> localVariableStores;

        private final Stack<BytecodeLocal> scopeTrackerForVariable;

        LoxLexicalScope(LoxLexicalScope parentScope) {
            this.parentScope = parentScope;
            this.localVariableStores = new HashMap<>();
            this.scopeTrackerForVariable = new Stack<>();
        }

        LoxLexicalScope() {
            this(null);
        }

        public void declare(String localVariableName, ParseTree ctx) {
            // Global scoping
            if (parentScope == null) {
                b.emitLoxDeclareGlobalVariable(localVariableName);
                return;
            }
            // Local scoping
            if (localVariableStores.get(localVariableName) != null) {
                throw LoxParseError.build(source, ctx, localVariableName);
            }
            localVariableStores.put(localVariableName, b.createLocal(localVariableName, null));
        }

        private BytecodeLocal lookupVariableName(String variableName) {
            // Temp save of current scope
            var scope = this;
            BytecodeLocal foundVariableStore;
            do {
                foundVariableStore = scope.localVariableStores.get(variableName);
                // Step into parent scope for next iteration.
                scope = scope.parentScope;
            } while (foundVariableStore == null && scope != null);
            return foundVariableStore;
        }

        /**
         * Begin store operation of variable, either in local or global scope.
         */
        public void beginStore(String variableName) {
            var variableStore = lookupVariableName(variableName);
            this.scopeTrackerForVariable.add(variableStore);
            if (variableStore != null) {
                // We found a local variable store.
                b.beginStoreLocal(variableStore);
            } else {
                // Store in global scope if nothing else applicable.
                b.beginLoxWriteGlobalVariable(variableName);
            }
        }

        /**
         * End store operation of variable, either in local or global scope.
         */
        public void endStore() {
            var variableStore = this.scopeTrackerForVariable.pop();
            if (variableStore != null) {
                // We found a local variable store.
                b.endStoreLocal();
            } else {
                // End of storing in global scope if nothing else applicable.
                b.endLoxWriteGlobalVariable();
            }
        }

        /**
         * Load variable value into scope, value either from a local or global scope.
         */
        public void loadIntoScope(String variableName) {
            var variableStore = lookupVariableName(variableName);
            if (variableStore != null) {
                // We found a local store for the variable.
                b.beginBlock();
                b.emitLoxCheckLocalDefined(variableStore);
                b.emitLoadLocal(variableStore);
                b.endBlock();
            } else {
                // Load from global scope.
                b.emitLoxReadGlobalVariable(variableName);
            }
        }
    }

    public static RootCallTarget parseLox(LoxLanguage language, Source source) {
        BytecodeParser<LoxBytecodeRootNodeGen.Builder> bytecodeParser = (b) -> {
            LoxBytecodeCompiler visitor = new LoxBytecodeCompiler(language, source, b);
            b.beginSource(source);
            LoxLexer lexer = new LoxLexer(CharStreams.fromString(source.getCharacters().toString()));
            LoxParser loxParser = new LoxParser(new CommonTokenStream(lexer));

            lexer.removeErrorListeners();
            loxParser.removeErrorListeners();
            BailoutErrorListener listener = new BailoutErrorListener(source);
            lexer.addErrorListener(listener);
            loxParser.addErrorListener(listener);

            loxParser.program().accept(visitor);
            b.endSource();
        };
        var config = LoxBytecodeRootNodeGen.newConfigBuilder().addSource().build();
        var nodes = LoxBytecodeRootNodeGen.create(language, config, bytecodeParser).getNodes();
        return nodes.get(nodes.size() - 1).getCallTarget();
    }

    private static final class BailoutErrorListener extends BaseErrorListener {
        private final Source source;

        BailoutErrorListener(Source source) {
            this.source = source;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                String msg, RecognitionException e) {
            throwParseError(source, line, charPositionInLine, (Token) offendingSymbol, msg);
        }

    }

    private static void throwParseError(Source source, int line, int charPositionInLine, Token token, String message) {
        int col = charPositionInLine + 1;
        String location = "-- line " + line + " col " + col + ": ";
        int length = token == null ? 1 : Math.max(token.getStopIndex() - token.getStartIndex(), 0);
        throw new LoxParseError(source, line, col, length,
                String.format("Error(s) parsing script:%n" + location + message));
    }

    private void beginAttribution(ParseTree tree) {
        beginAttribution(getStartIndex(tree), getEndIndex(tree));
    }

    private static int getEndIndex(ParseTree tree) {
        if (tree instanceof ParserRuleContext ctx) {
            return ctx.getStop().getStopIndex();
        } else if (tree instanceof TerminalNode node) {
            return node.getSymbol().getStopIndex();
        } else {
            throw new AssertionError("unknown tree type: " + tree);
        }
    }

    private static int getStartIndex(ParseTree tree) {
        if (tree instanceof ParserRuleContext ctx) {
            return ctx.getStart().getStartIndex();
        } else if (tree instanceof TerminalNode node) {
            return node.getSymbol().getStartIndex();
        } else {
            throw new AssertionError("unknown tree type: " + tree);
        }
    }

    private void beginAttribution(int start, int end) {
        int length = end - start + 1;
        assert length >= 0;
        b.beginSourceSection(start, length);
    }

    private void endAttribution() {
        b.endSourceSection();
    }

    private LoxBytecodeCompiler(LoxLanguage language, Source source, LoxBytecodeRootNodeGen.Builder builder) {
        this.language = language;
        this.source = source;
        this.b = builder;
    }

    /**
     * Returns, whether execution takes place in repl environment.
     * 
     * @return {@link Boolean} isRepl
     */
    private boolean getIsRepl() {
        // Use env from context to retrieve whether execution takes place in repl
        // environment.
        // Why don't we use Source#isInteractive()? Because when using this,
        // somehow also the exit code (0) is printed for every new statement.
        Env env = LoxContext.get(null).getEnv();
        return Boolean.valueOf(env.getEnvironment().getOrDefault("isRepl", "false"));
    }

    @Override
    public Void visitProgram(ProgramContext ctx) {
        b.beginRoot();
        var result = super.visitProgram(ctx);
        b.beginReturn();
        b.emitLoadConstant(0);
        b.endReturn();
        b.endRoot();
        return result;
    }

    @Override
    public Void visitPrintStmt(PrintStmtContext ctx) {
        beginAttribution(ctx);
        b.beginLoxPrint();
        var result = super.visitPrintStmt(ctx);
        b.endLoxPrint();
        endAttribution();
        return result;
    }

    @Override
    public Void visitBoolean(BooleanContext ctx) {
        return super.visitBoolean(ctx);
    }

    @Override
    public Void visitTrue(TrueContext ctx) {
        b.emitLoadConstant(true);
        return super.visitTrue(ctx);
    }

    @Override
    public Void visitFalse(FalseContext ctx) {
        b.emitLoadConstant(false);
        return super.visitFalse(ctx);
    }

    @Override
    public Void visitString(StringContext ctx) {
        // Remove quotes and convert to TruffleString
        var ts = TruffleString.fromJavaStringUncached(
                ctx.getText().substring(1, ctx.getText().length() - 1), TruffleString.Encoding.UTF_8);
        b.emitLoadConstant(ts);
        return super.visitString(ctx);
    }

    @Override
    public Void visitNil(NilContext ctx) {
        b.emitLoadConstant(Nil.INSTANCE);
        return super.visitNil(ctx);
    }

    @Override
    public Void visitNumber(NumberContext ctx) {
        b.emitLoadConstant(new LoxNumber(ctx.getText()));
        return super.visitNumber(ctx);
    }

    @Override
    public Void visitUnary(UnaryContext ctx) {
        String UNARY_INVERT_OPERATOR = "!";
        String UNARY_NEGATE_OPERATOR = "-";

        beginAttribution(ctx);
        String text = ctx.getText();

        Void unaryResult;
        if (text.startsWith(UNARY_INVERT_OPERATOR)) {
            // Invert
            b.beginLoxInvert();
            unaryResult = visitUnary(ctx.unary());
            b.endLoxInvert();
        } else if (text.startsWith(UNARY_NEGATE_OPERATOR)) {
            // Negate
            b.beginLoxNegate();
            unaryResult = visitUnary(ctx.unary());
            b.endLoxNegate();
        } else {
            // Primary
            unaryResult = super.visitUnary(ctx);
        }

        endAttribution();
        return unaryResult;

    }

    @Override
    public Void visitPrimary(PrimaryContext ctx) {
        if (ctx.getText().startsWith("(") && ctx.getText().endsWith(")")) {
            return visitExpression(ctx.expression());
        } else {
            return super.visitPrimary(ctx);
        }
    }

    @Override
    public Void visitFactor(FactorContext ctx) {
        beginAttribution(ctx);
        // Collect operations in reverse order
        Deque<String> operations = new ArrayDeque<>();

        for (int i = ctx.getChildCount() - 2; i >= 0; i -= 2) {
            var operation = ctx.getChild(i);
            switch (operation.getText()) {
                case "*":
                    b.beginLoxMultiply();
                    break;
                case "/":
                    b.beginLoxDivide();
                    break;
                default:
                    break;
            }
            operations.addFirst(operation.getText());
        }
        visitUnary(ctx.unary(0));
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            visitUnary(ctx.unary((i + 1) / 2));
            // Apply operations in reverse order that it matches the order of the operations
            switch (operations.removeFirst()) {
                case "*":
                    b.endLoxMultiply();
                    break;
                case "/":
                    b.endLoxDivide();
                    break;
                default:
                    break;
            }
        }
        endAttribution();
        return null;
    }

    @Override
    public Void visitTerm(TermContext ctx) {
        // Collect operations in reverse order
        beginAttribution(ctx);
        Deque<String> operations = new ArrayDeque<>();

        for (int i = ctx.getChildCount() - 2; i >= 0; i -= 2) {
            var operation = ctx.getChild(i);
            switch (operation.getText()) {
                case "+":
                    b.beginLoxAdd();
                    break;
                case "-":
                    b.beginLoxSubtract();
                    break;
                default:
                    break;
            }
            operations.addFirst(operation.getText());
        }
        visitFactor(ctx.factor(0));
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            visitFactor(ctx.factor((i + 1) / 2));
            // Apply operations in reverse order that it matches the order of the operations
            switch (operations.removeFirst()) {
                case "+":
                    b.endLoxAdd();
                    break;
                case "-":
                    b.endLoxSubtract();
                    break;
                default:
                    break;
            }
        }
        endAttribution();
        return null;
    }

    @Override
    public Void visitLogic_or(Logic_orContext ctx) {
        beginAttribution(ctx);

        for (int i = ctx.getChildCount() - 2; i >= 0; i -= 2) {
            // For every 'or' we encounter (its the only operator possible here), beginn new
            // lox or.
            b.beginLoxOr();
        }
        visitLogic_and(ctx.logic_and(0));
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            visitLogic_and(ctx.logic_and((i + 1) / 2));
            // End or statements
            b.endLoxOr();
        }

        endAttribution();
        return null;
    }

    @Override
    public Void visitLogic_and(Logic_andContext ctx) {
        beginAttribution(ctx);

        for (int i = ctx.getChildCount() - 2; i >= 0; i -= 2) {
            // For every 'and' we encounter (its the only operator possible here), beginn
            // new lox and.
            b.beginLoxAnd();
        }
        visitEquality(ctx.equality(0));
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            visitEquality(ctx.equality((i + 1) / 2));
            // End and statements
            b.endLoxAnd();
        }

        endAttribution();
        return null;
    }

    @Override
    public Void visitEquality(EqualityContext ctx) {
        // Collect equality operators in reverse order
        Deque<String> operators = new ArrayDeque<>();

        beginAttribution(ctx);

        for (int i = ctx.getChildCount() - 2; i >= 0; i -= 2) {
            var operation = ctx.getChild(i);
            switch (operation.getText()) {
                case "!=":
                    b.beginLoxInequal();
                    break;
                case "==":
                    b.beginLoxEqual();
                    break;
                default:
                    break;
            }
            operators.addFirst(operation.getText());
        }
        visitComparison(ctx.comparison(0));
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            visitComparison(ctx.comparison((i + 1) / 2));
            // Apply equality operators in reverse order that it matches the order of the
            // operators
            switch (operators.removeFirst()) {
                case "!=":
                    b.endLoxInequal();
                    break;
                case "==":
                    b.endLoxEqual();
                    break;
                default:
                    break;
            }
        }

        endAttribution();
        return null;
    }

    @Override
    public Void visitComparison(ComparisonContext ctx) {
        // Collect comparison operators in reverse order
        Deque<String> operators = new ArrayDeque<>();

        beginAttribution(ctx);

        for (int i = ctx.getChildCount() - 2; i >= 0; i -= 2) {
            var operation = ctx.getChild(i);
            switch (operation.getText()) {
                case ">=":
                    b.beginLoxGreaterOrEqual();
                    break;
                case ">":
                    b.beginLoxGreater();
                    break;
                case "<=":
                    b.beginLoxLessOrEqual();
                    break;
                case "<":
                    b.beginLoxLess();
                    break;
                default:
                    break;
            }
            operators.addFirst(operation.getText());
        }
        visitTerm(ctx.term(0));
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            visitTerm(ctx.term((i + 1) / 2));
            // Apply comparison operators in reverse order that it matches the order of the
            // operators
            switch (operators.removeFirst()) {
                case ">=":
                    b.endLoxGreaterOrEqual();
                    break;
                case ">":
                    b.endLoxGreater();
                    break;
                case "<=":
                    b.endLoxLessOrEqual();
                    break;
                case "<":
                    b.endLoxLess();
                    break;
                default:
                    break;
            }
        }

        endAttribution();
        return null;
    }

    @Override
    public Void visitVarDecl(VarDeclContext ctx) {
        var variableName = ctx.IDENTIFIER().getText();
        // Declare in scope
        lexicalScope.declare(variableName, ctx);
        if (ctx.expression() != null) {
            // If an expression is following, define with assigned value (store).
            lexicalScope.beginStore(variableName);
            visit(ctx.expression());
            lexicalScope.endStore();
        }
        return null;
    }

    @Override
    public Void visitVariableExpr(VariableExprContext ctx) {
        lexicalScope.loadIntoScope(ctx.IDENTIFIER().getText());
        return null;
    }

    @Override
    public Void visitAssignment(AssignmentContext ctx) {
        final boolean isAssignment = ctx.IDENTIFIER() != null;
        String variableName = null;
        if (isAssignment) {
            // For grouping the storing and the emition of the value together.
            b.beginBlock();
            variableName = ctx.IDENTIFIER().getText();
            // Directly begin storing (defining)
            lexicalScope.beginStore(variableName);
        }
        // Emit value to assign in super operation
        super.visitAssignment(ctx);
        if (isAssignment) {
            // End storing (defining)
            lexicalScope.endStore();
            // For directly returning the assigned value from the assignment.
            lexicalScope.loadIntoScope(variableName);
            b.endBlock();
        }
        return null;
    }

    @Override
    public Void visitBlock(BlockContext ctx) {
        b.beginBlock();
        lexicalScope = new LoxLexicalScope(lexicalScope);
        super.visitBlock(ctx);
        lexicalScope = lexicalScope.parentScope;
        b.endBlock();
        return null;
    }

    @Override
    public Void visitExprStmt(ExprStmtContext ctx) {
        boolean isRepl = getIsRepl();
        if (isRepl) {
            // If in repl environment print the value that is it emitted in the following
            // code.
            b.beginLoxPrint();
        }
        // Regularly visit the expression in the statement.
        visitExpression(ctx.expression());
        if (isRepl) {
            // End lox print if in repl environment.
            b.endLoxPrint();
        }
        return null;
    }

    @Override
    public Void visitIfStmt(IfStmtContext ctx) {
        if (ctx.alt == null) {
            b.beginIfThen();
            beginAttribution(ctx.condition);
            b.beginLoxIsTruthy();
            visit(ctx.condition);
            b.endLoxIsTruthy();
            endAttribution();
            visit(ctx.then);
            b.endIfThen();
        } else {
            b.beginIfThenElse();
            beginAttribution(ctx.condition);
            b.beginLoxIsTruthy();
            visit(ctx.condition);
            b.endLoxIsTruthy();
            endAttribution();
            visit(ctx.then);
            visit(ctx.alt);
            b.endIfThenElse();
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(WhileStmtContext ctx) {
        b.beginWhile();
        beginAttribution(ctx.condition);
        b.beginLoxIsTruthy();
        visit(ctx.condition);
        b.endLoxIsTruthy();
        endAttribution();
        visit(ctx.body);
        b.endWhile();
        return null;
    }

    @Override
    public Void visitForStmt(ForStmtContext ctx) {
        ParserRuleContext init = ctx.loopVar;
        if (init == null) {
            init = ctx.exprStmt();
        }
        if (init != null) {
            visit(init);
        }
        b.beginWhile();
        beginAttribution(ctx.condition);
        b.beginLoxIsTruthy();
        visit(ctx.condition);
        b.endLoxIsTruthy();
        endAttribution();
        b.beginBlock();
        visit(ctx.body);
        visit(ctx.increment);
        b.endBlock();
        b.endWhile();
        return null;
    }

    @Override
    public Void visitForOfStmt(ForOfStmtContext ctx) {
        // Visit declaration of element var.
        visitVarDecl(ctx.elementVar);
        beginAttribution(ctx);
        // Check if variable to iterate through is actually an array.
        b.beginLoxIsArray();
        visitVariableExpr(ctx.toIterate);
        b.endLoxIsArray();
        endAttribution();
        // Begin loop operation that retrieves every element of array.
        b.beginWhile();
        beginAttribution(ctx);
        // Check if iterator through has next element.
        b.beginLoxArrayHasNext();
        visitVariableExpr(ctx.toIterate);
        b.endLoxArrayHasNext();
        endAttribution();
        b.beginBlock();
        // Assign elementVar to next value.
        lexicalScope.beginStore(ctx.elementVar.IDENTIFIER().getText());
        // Retrieve next element from iterator to store in elementVar.
        b.beginLoxArrayGetNext();
        visitVariableExpr(ctx.toIterate);
        b.endLoxArrayGetNext();
        lexicalScope.endStore();
        // Visit the actual body of for-of loop.
        visit(ctx.body);
        b.endBlock();
        b.endWhile();
        return null;
    }

    @Override
    public Void visitForInStmt(ForInStmtContext ctx) {
        // Visit declaration of index var.
        visitVarDecl(ctx.indexVar);
        beginAttribution(ctx);
        // Check if variable to iterate through is actually an array.
        b.beginLoxIsArray();
        visitVariableExpr(ctx.toIterate);
        b.endLoxIsArray();
        endAttribution();
        // Begin loop operation that retrieves every element of array.
        b.beginWhile();
        beginAttribution(ctx);
        // Check if iterator through has next element.
        b.beginLoxArrayHasNext();
        visitVariableExpr(ctx.toIterate);
        b.endLoxArrayHasNext();
        endAttribution();
        b.beginBlock();
        // Assign indexVar to next index.
        lexicalScope.beginStore(ctx.indexVar.IDENTIFIER().getText());
        // Retrieve next index from iterator to store in indexVar.
        b.beginLoxArrayGetNextIndex();
        visitVariableExpr(ctx.toIterate);
        b.endLoxArrayGetNextIndex();
        lexicalScope.endStore();
        // Actually move iterator to the next element, but ignore its result here.
        b.beginLoxArrayGetNext();
        visitVariableExpr(ctx.toIterate);
        b.endLoxArrayGetNext();
        // Visit the actual body of for-in loop.
        visit(ctx.body);
        b.endBlock();
        b.endWhile();
        return null;
    }

    @Override
    public Void visitArray(ArrayContext ctx) {
        if (ctx.expression().isEmpty()) {
            // No direct values, so just initialize and emit new empty array.
            b.emitLoxNewArray();
            return null;
        }
        // Otherwise, we have values to initialize with.
        // Naively do sth like this (here illustrated with two values):
        //
        // b.beginLoxAppendArray();
        //    b.beginLoxAppendArray();
        //       b.emitLoxNewArray();
        //       visit(ctx.expression(0));
        //    b.endLoxAppendArray();
        //    visit(ctx.expression(1));
        // b.endLoxAppendArray();        
        //
        for (int exprIndex = ctx.expression().size() - 1; exprIndex >= 0; exprIndex--) {
            b.beginLoxAppendArray();
            if (exprIndex == 0) {
                // Initialize and emit new empty array for the most inner append statement.
                b.emitLoxNewArray();
            }
        }
        for (int exprIndex = 0; exprIndex < ctx.expression().size(); exprIndex++) {
            visitExpression(ctx.expression(exprIndex));
            b.endLoxAppendArray();
        }
        return null;
    }

    @Override
    public Void visitArrayExpr(ArrayExprContext ctx) {
        b.beginLoxReadArray();
        visit(ctx.left);
        visit(ctx.index);
        b.endLoxReadArray();
        return null;
    }

    @Override
    public Void visitArrayAssignment(ArrayAssignmentContext ctx) {
        if (ctx.other != null) {
            return visit(ctx.other);
        }
        b.beginLoxWriteArray();
        visit(ctx.left);
        visit(ctx.index);
        visit(ctx.right);
        b.endLoxWriteArray();
        return null;
    }

    /**
     * Enters a function by creating a new variable scope and return the parameter variables.
     */
    private final List<String> retrieveParameterNames(FunctionContext ctx) {
        List<String> parameterNames = new ArrayList<>();
        ParametersContext parameters = ctx.parameters();
        if (parameters != null) {
            for (int i = 0; i < parameters.IDENTIFIER().size(); i++) {
                TerminalNode param = parameters.IDENTIFIER(i);
                parameterNames.add(param.getText());
            }
        }
        return parameterNames;
    }

    @Override
    public Void visitFunDeclStmt(FunDeclStmtContext ctx) {
        FunctionContext function = ctx.function();
        String funName = function.IDENTIFIER().getText();
        // Declare function as its own variable in the outer scope.
        lexicalScope.declare(funName, ctx);
        // Begin a new, separate call target.
        b.beginRoot();
        // Group all function operations together.
        b.beginBlock();
        // Create new variable scope for function.
        lexicalScope = new LoxLexicalScope(lexicalScope);
        // Retrieve parameter names.
        List<String> parameterNames = retrieveParameterNames(function);
        for (int i = 0; i < parameterNames.size(); i++) {
            var paramName = parameterNames.get(i);
            // Declare parameter as local variable in the function scope.
            lexicalScope.declare(paramName, ctx);
            // Assign argument (value) to the variable (= Define)
            lexicalScope.beginStore(paramName);
            b.emitLoxLoadFunctionArgument(i);
            lexicalScope.endStore();
        }
        // Group function body execution and exiting the function together.
        b.beginBlock();
        // Execute function body.
        visit(ctx.function().block());
        // Reset the variable scope to the outer scope.
        lexicalScope = lexicalScope.parentScope;
        // End all grouping.
        b.endBlock();
        b.endBlock();
        // Begin returning of the separate call target to return to the root execution.
        b.beginReturn();
        // Default return value is nil (if no return statement was executed earlier).
        b.emitLoadConstant(Nil.INSTANCE);
        b.endReturn();
        // End encapsulation of function in separate call target.
        LoxRootNode node = b.endRoot();
        // Assign the actual function object to the just declared variable.
        lexicalScope.beginStore(funName);
        b.emitLoxCreateFunction(funName, node.getCallTarget());
        lexicalScope.endStore();
        return null;
    }
    
    @Override
    public Void visitReturnStmt(ReturnStmtContext ctx) {
        b.beginReturn();
        if (ctx.expression() != null) {
            // Emit the actual return value by visiting the expression.
            visit(ctx.expression());
        } else {
            // Default return value is our nil.
            b.emitLoadConstant(Nil.INSTANCE);
        }
        b.endReturn();
        return null;
    }

    @Override
    public Void visitCall(CallContext ctx) {
        var calls = ctx.callArguments();
        for (int i = 0; i < calls.size(); i++) {
            // In order to support f(x)(y)(z) and such stuff.
            b.beginLoxCallFunction();
        }
        super.visit(ctx.primary());
        for (CallArgumentsContext callArguments : calls) {
            ArgumentsContext args = callArguments.arguments();
            if (args != null) {
                List<ExpressionContext> expressions = args.expression();
                for (int i = 0; i < expressions.size(); i++) {
                    visit(expressions.get(i));
                }
            }
            b.endLoxCallFunction();
        }
        return null;
    }
}
