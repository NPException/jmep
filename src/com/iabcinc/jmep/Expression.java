/* * JMEP - Java Mathematical Expression Parser. * Copyright (C) 1999  Jo Desmet *  * This library is free software; you can redistribute it and/or * modify it under the terms of the GNU Lesser General Public * License as published by the Free Software Foundation; either * version 2.1 of the License, or any later version. *  * This library is distributed in the hope that it will be useful, * but WITHOUT ANY WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU * Lesser General Public License for more details. *  * You should have received a copy of the GNU Lesser General Public * License along with this library; if not, write to the Free Software * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA *  * You can contact the Original submitter of this library by * email at: Jo_Desmet@yahoo.com. *  */package com.iabcinc.jmep;import com.iabcinc.jmep.hooks.Constant;import com.iabcinc.jmep.hooks.Unit;import com.iabcinc.jmep.hooks.Variable;import com.iabcinc.jmep.hooks.Function;import java.util.LinkedList;import java.util.Iterator;import java.util.EmptyStackException;import java.text.StringCharacterIterator;import static com.iabcinc.jmep.tokens.BINToken.Operator.*;import com.iabcinc.jmep.tokens.BINToken;import com.iabcinc.jmep.tokens.FNCToken;import com.iabcinc.jmep.tokens.Token;import com.iabcinc.jmep.tokens.UNAToken;import com.iabcinc.jmep.tokens.UNIToken;import com.iabcinc.jmep.tokens.VALToken;import com.iabcinc.jmep.tokens.VARToken;import java.util.Deque;/* * TODO * - define additional strict operators for LT,GT,EQ and NEQ for use on doubles, *   currently it is always strict for those operators. * - implement additional optimalizations. * - implement complex values (and operators on them). *//** * The container for a Mathematical Expression. This class enables the * user to define a mathematical expression and evaluate it with support * for a precompile to enable faster execution for within a loop.<p> * * The expression string supports variables and functions, number expressions * (resulting in either an int or double) and string expressions.<p> * * Literal strings has to be quited using the double quote (").<p> * * Supported operators are: <code>( ) + - * / ^ and or xor &amp; | * &lt; &lt;= &gt; &gt;= = &lt;&gt; % not inv</code> * and follows mostly the rules as in most programming languages.<p> * * Be carefull with the use of the division parameter (<code>/</code>), when * both operands are of the type Integer, then the result will also be an * Integer when used in the default strict mode. You can overrule this by * multiplying the first operand with 1.0. * * @author <a href="mailto:jo_desmet@yahoo.com>Jo Desmet</a> * @see com.iabcinc.jmep.Environment */public class Expression {    private static final int D_TokenToOS   = 0x0001; /* Move current token to Operator Stack, next token is taken as current */    private static final int D_TokenToRS   = 0x0002; /* Move current token to Result (RPN) Stack, next token is taken as current */    private static final int D_NextToken   = 0x0004; /* Skip Current Token and take next as current */    private static final int D_PopOS       = 0x0008; /* Remove token from Operator Stack */    private static final int D_PopRS       = 0x0010; /* Remove token from Result Stack */    private static final int D_OSToRS      = 0x0020; /* Move token from Operator Stack to Result Stack */    private static final int D_Internal    = 0x0040; /* Internal Error <- status not possible */    private static final int D_Syntax      = 0x0080; /* Syntax Error */    private static final int D_Done        = 0x0100; /* Done, Result after Evaluating Result Stack */    private static final int D_CheckFNCPar = 0x0200; /* Check function parameters (number and type) */    private static final int D_IncParCount = 0x0400; /* Increase parameter count */    private static final int D_Precedence  = 0x0800; /* Two binary operators following each other */    private static final int D_PushParCount= 0x1000; /* Stacking the parameter count (nested functions) */    private static final int D_PopParCount = 0x2000; /* Unstacking the parameter count (nested functions) */        private static final int [][] arr_kDispatch = new int [][] {            /* OS TP - TOKEN */            /*****************/            {  /* MRK - MRK */ D_Done                , /*       OPA */ D_TokenToOS                , /*       FNC */ D_TokenToOS|D_PushParCount                , /*       CMA */ D_Syntax                , /*       UNA */ D_TokenToOS                , /*       BIN */ D_TokenToOS                , /*       VAL */ D_TokenToRS                , /*       VAR */ D_TokenToRS                , /*       CPA */ D_Syntax                , /*       ERR */ D_Syntax                , /*       UNI */ D_TokenToRS            },{/* OPA - MRK */ D_Syntax                , /*       OPA */ D_TokenToOS                , /*       FNC */ D_TokenToOS|D_PushParCount                , /*       CMA */ D_Syntax                , /*       UNA */ D_TokenToOS                , /*       BIN */ D_TokenToOS                , /*       VAL */ D_TokenToRS                , /*       VAR */ D_TokenToRS                , /*       CPA */ D_PopOS|D_NextToken                , /*       ERR */ D_Syntax                , /*       UNI */ D_TokenToRS            },{/* FNC - MRK */ D_Syntax                , /*       OPA */ D_TokenToOS                , /*       FNC */ D_TokenToOS|D_PushParCount                , /*       CMA */ D_TokenToOS                , /*       UNA */ D_TokenToOS                , /*       BIN */ D_TokenToOS                , /*       VAL */ D_TokenToRS                , /*       VAR */ D_TokenToRS                , /*       CPA */ D_NextToken|D_OSToRS|D_PopParCount|D_IncParCount|D_CheckFNCPar                , /*       ERR */ D_Syntax                , /*       UNI */ D_TokenToRS            },{/* CMA - MRK */ D_Syntax                , /*       OPA */ D_TokenToOS                , /*       FNC */ D_TokenToOS|D_PushParCount                , /*       CMA */ D_TokenToOS                , /*       UNA */ D_TokenToOS                , /*       BIN */ D_TokenToOS                , /*       VAL */ D_TokenToRS                , /*       VAR */ D_TokenToRS                , /*       CPA */ D_PopOS|D_IncParCount                , /*       ERR */ D_Syntax                , /*       UNI */ D_TokenToRS            },{/* UNA - MRK */ D_OSToRS                , /*       OPA */ D_TokenToOS                , /*       FNC */ D_TokenToOS|D_PushParCount                , /*       CMA */ D_OSToRS                , /*       UNA */ D_TokenToOS                , /*       BIN */ D_OSToRS|D_TokenToOS                , /*       VAL */ D_TokenToRS                , /*       VAR */ D_TokenToRS                , /*       CPA */ D_OSToRS                , /*       ERR */ D_Syntax                , /*       UNI */ D_TokenToRS            },{/* BIN - MRK */ D_OSToRS                , /*       OPA */ D_TokenToOS                , /*       FNC */ D_TokenToOS|D_PushParCount                , /*       CMA */ D_OSToRS                , /*       UNA */ D_TokenToOS                , /*       BIN */ D_Precedence /* OS >= Token => D_OSToRS|D_TokenToOS */                /* OS <  Token => D_TokenToOS */                , /*       VAL */ D_TokenToRS                , /*       VAR */ D_TokenToRS                , /*       CPA */ D_OSToRS                , /*       ERR */ D_Syntax                , /*       UNI */ D_TokenToRS            },{/* VAL - MRK */ D_Internal                , /*       OPA */ D_Internal                , /*       FNC */ D_Internal                , /*       CMA */ D_Internal                , /*       UNA */ D_Internal                , /*       BIN */ D_Internal                , /*       VAL */ D_Internal                , /*       VAR */ D_Internal                , /*       CPA */ D_Internal                , /*       ERR */ D_Internal                , /*       UNI */ D_Internal            },{/* VAR - MRK */ D_Internal                , /*       OPA */ D_Internal                , /*       FNC */ D_Internal                , /*       CMA */ D_Internal                , /*       UNA */ D_Internal                , /*       BIN */ D_Internal                , /*       VAL */ D_Internal                , /*       VAR */ D_Internal                , /*       CPA */ D_Internal                , /*       ERR */ D_Internal                , /*       UNI */ D_Internal            },{/* CPA - MRK */ D_Syntax                , /*       OPA */ D_Syntax                , /*       FNC */ D_Syntax                , /*       CMA */ D_Syntax                , /*       UNA */ D_Syntax                , /*       BIN */ D_Syntax                , /*       VAL */ D_Syntax                , /*       VAR */ D_Syntax                , /*       CPA */ D_Syntax                , /*       ERR */ D_Syntax                , /*       UNI */ D_Syntax            },{/* ERR - MRK */ D_Syntax                , /*       OPA */ D_Syntax                , /*       FNC */ D_Syntax                , /*       CMA */ D_Syntax                , /*       UNA */ D_Syntax                , /*       BIN */ D_Syntax                , /*       VAL */ D_Syntax                , /*       VAR */ D_Syntax                , /*       CPA */ D_Syntax                , /*       ERR */ D_Syntax                , /*       UNI */ D_Syntax            },{/* UNI - MRK */ D_Internal                , /*       OPA */ D_Internal                , /*       FNC */ D_Internal                , /*       CMA */ D_Internal                , /*       UNA */ D_Internal                , /*       BIN */ D_Internal                , /*       VAL */ D_Internal                , /*       VAR */ D_Internal                , /*       CPA */ D_Internal                , /*       ERR */ D_Internal                , /*       UNI */ D_Internal            } }; /*****************/    private String expression;    private Environment environment;    private Deque<com.iabcinc.jmep.tokens.Token> tokenList;    private Deque<com.iabcinc.jmep.tokens.Token> rpnStack;    private boolean strict;        /**     * Constructs a mathematical expression from a String. This will do all     * initial compiling of the string with all needed optimizations.     * Expressions are evaluated strictly by default.     * @param expression the string containing the mathematical expression.     * @see com.iabcinc.jmep.XExpression     */    public Expression(String expression) throws XExpression {        this(expression,new Environment(),true);    }        /**     * Constructs a mathematical expression from a String using a specific     * user environment. This will do all initial compiling of the string with     * all necesairy optimalizations. Expressions are evaluated strictly by default.     * @param expression the string containing the mathematical expression.     * @param environment the environment that contains all user defined variables,     * functions and units.     * @see com.iabcinc.jmep.Environment     * @see com.iabcinc.jmep.XExpression     */    public Expression(String expression,Environment environment) throws XExpression {        this(expression,environment,true);    }        /**     * Constructs a mathematical expression from a String using a specific     * user environment. This will do all initial compiling of the string with     * all necesairy optimalizations.     * @param expression the string containing the mathematical expression.     * @param environment the environment that contains all user defined variables,     * functions and units.     * @param strict when true, the expressions will be interpreted strictly,     * meaning that a division of 2 Integer values will return an Integer value.     * @see com.iabcinc.jmep.Environment     * @see com.iabcinc.jmep.XExpression     */    public Expression(String expression,Environment environment,boolean strict) throws XExpression {        this.expression = expression;        this.environment = environment;        this.strict = strict;        tokenize();        compile();        /* optimize(); */        /*         * TODO:         *  - implement a method to optimize the resultint RPNStack.         *    call this method 'optimize()'. This can wait, and should have         *    low priority because JMEP can work without it.         */    }        private static String parseIdentifier(StringCharacterIterator iterString) {        StringBuilder identifier = new StringBuilder();        char cc = iterString.current();                if (!Character.isUnicodeIdentifierStart(cc)) return null;        identifier.append(cc);        cc = iterString.next();        while (Character.isIdentifierIgnorable(cc)) cc = iterString.next();        while (Character.isUnicodeIdentifierPart(cc) || cc == '.') {            identifier.append(cc);            cc = iterString.next();            while (Character.isIdentifierIgnorable(cc)) cc = iterString.next();        }        return identifier.toString();    }        private static Number parseNumber(StringCharacterIterator iterString) {        char cc = iterString.current();                if (Character.isDigit(cc) || cc == '.') {            int intValue = 0;            while (Character.isDigit(cc)) {                intValue *= 10;                intValue += Character.digit(cc,10);                cc = iterString.next();            }            if (cc != '.' && cc != 'e' && cc != 'E') {                /* it is an int */                return new Integer(intValue);            }            else {                /* it is a double */                double doubleValue = intValue;                if (cc == '.') {                    double scale = 1.0;                    cc = iterString.next();                    while (Character.isDigit(cc)) {                        scale *= 0.1;                        doubleValue += scale * Character.digit(cc,10);                        cc = iterString.next();                    }                }                if (cc == 'e' || cc == 'E') {                    boolean bPositive = true;                    intValue = 0;                    cc = iterString.next();                    if (!Character.isDigit(cc)) {                        if (cc == '+') {                            bPositive = true;                            cc = iterString.next();                        }                        else if (cc == '-') {                            bPositive = false;                            cc = iterString.next();                        }                        else {                            return null; /* not a legal number */                        }                    }                    while (Character.isDigit(cc)) {                        intValue *= 10;                        intValue += Character.digit(cc,10);                        cc = iterString.next();                    }                    doubleValue *= Math.pow(10,bPositive?intValue:-intValue);                }                return new Double(doubleValue);            }        }        return null;    }        private String parseString(StringCharacterIterator iterString) {        char cc = iterString.current();        StringBuilder string = new StringBuilder();                if (cc == '"') {            cc = iterString.next();            while (cc != StringCharacterIterator.DONE && cc != '"') {                                if (cc == '\\') {                    int iSavePos = iterString.getIndex();                    char nc = iterString.next();                    iterString.setIndex(iSavePos);                    if (nc == '"') cc = iterString.next();                    else if (nc == '\\') cc = iterString.next();                }                string.append(cc);                cc = iterString.next();            }            if (cc == '"') cc = iterString.next();            return string.toString();        }        return null;    }    private void tokenize() throws XExpression {        char cc;        StringCharacterIterator iterString = new StringCharacterIterator(this.expression);                this.tokenList = new java.util.LinkedList<com.iabcinc.jmep.tokens.Token>();        this.tokenList.add(new Token(Token.MRK,0));                cc = iterString.first();                        while (true) {            while (Character.isWhitespace(cc)) cc = iterString.next();                        switch (cc) {	            case StringCharacterIterator.DONE:	                this.tokenList.add(new Token(Token.MRK,iterString.getIndex()));	                return;	            case '(':	                this.tokenList.add(new Token(Token.OPA,iterString.getIndex()));	                cc = iterString.next();	                continue;	            case ')':	                this.tokenList.add(new Token(Token.CPA,iterString.getIndex()));	                cc = iterString.next();	                continue;	            case ',':	                this.tokenList.add(new Token(Token.CMA,iterString.getIndex()));	                cc = iterString.next();	                continue;            }                        if (Character.isUnicodeIdentifierStart(cc)) {                int identifierPosition = iterString.getIndex();                String identifier = parseIdentifier(iterString);                cc = iterString.current();                                if (identifier == null) throw new XIllegalStatus(identifierPosition);                Token lastToken = tokenList.peekLast();                if (                        lastToken.getKindOfToken() == Token.VAL ||                        lastToken.getKindOfToken() == Token.VAR ||                        lastToken.getKindOfToken() == Token.UNI ||                        lastToken.getKindOfToken() == Token.CPA                ) {                    /* Check if the identifier is a Binary Operator */                    if (identifier.equalsIgnoreCase("and")) {                        this.tokenList.add(new BINToken(LAND,identifierPosition));                        continue;                    }                    if (identifier.equalsIgnoreCase("xor")) {                        this.tokenList.add(new BINToken(XOR,identifierPosition));                        continue;                    }                    if (identifier.equalsIgnoreCase("or")) {                        this.tokenList.add(new BINToken(LOR,identifierPosition));                        continue;                    } else {                        /* Else it must be a unit operator */                        Unit oUnit = (Unit)this.environment.getUnit(identifier);                        if (oUnit == null)                            this.tokenList.add(new UNIToken(identifier,identifierPosition));                        else                            this.tokenList.add(new UNIToken(identifier,oUnit,identifierPosition));                        continue;                    }                } else if (lastToken.getKindOfToken() != Token.UNI) {                    /* Check if the identifier is an unary operator */                    if (identifier.equalsIgnoreCase("not")) {                        this.tokenList.add(new UNAToken(UNAToken.NOT,identifierPosition));                        continue;                    }                    if (identifier.equalsIgnoreCase("inv")) {                        this.tokenList.add(new UNAToken(UNAToken.INV,identifierPosition));                        continue;                    }                }                while (Character.isWhitespace(cc)) cc = iterString.next();                if (cc == '(') {                    /* it is a function */                    Function oFunction = (Function)this.environment.getFunction(identifier);                    cc = iterString.next();                    if (oFunction == null)                        this.tokenList.add(new FNCToken(identifier,identifierPosition));                    else                        this.tokenList.add(new FNCToken(identifier,oFunction,identifierPosition));                    continue;                }                else {                    /* it is a variable */                    Variable oVariable = this.environment.getVariable(identifier);                    if (oVariable == null)                      this.tokenList.add(new VARToken(identifier,identifierPosition));                    else if (oVariable instanceof Constant)                      this.tokenList.add(new VALToken(oVariable.getValue(),identifierPosition));                    else                      this.tokenList.add(new VARToken(identifier,(Variable)oVariable,identifierPosition));                    //else if (oVariable instanceof Integer)                    //    this.tokenList.addElement(new VARToken(identifier,((Integer)oVariable).intValue(),identifierPosition));                    //else if (oVariable instanceof Double)                    //    this.tokenList.addElement(new VARToken(identifier,((Double)oVariable).doubleValue(),identifierPosition));                    //else if (oVariable instanceof String)                    //    this.tokenList.addElement(new VARToken(identifier,(String)oVariable,identifierPosition));                    //else                    //    throw new XIllegalStatus(identifierPosition);                    continue;                }            }                        if (Character.isDigit(cc) || cc == '.') {                 /* is numerical */                Number oNumber;                int iNumberPos = iterString.getIndex();                                oNumber = parseNumber(iterString);                cc = iterString.current();                if (oNumber == null)                    throw new XIllegalStatus(iNumberPos);                else if (oNumber instanceof Integer)                    this.tokenList.add(new VALToken(((Integer)oNumber).intValue(),iNumberPos));                else if (oNumber instanceof Double)                    this.tokenList.add(new VALToken(((Double)oNumber).doubleValue(),iNumberPos));                else                    throw new XIllegalStatus(iNumberPos);                continue;            }                        if (cc == '"') {                int iStringPos = iterString.getIndex();                String sValue = parseString(iterString);                cc = iterString.current();                if (sValue == null) throw new XIllegalStatus(iStringPos);                this.tokenList.add(new VALToken(sValue,iStringPos));                continue;            }                        switch (cc) {            case '^':                this.tokenList.add(new BINToken(POW,iterString.getIndex()));                cc = iterString.next();                continue;            case '*':                this.tokenList.add(new BINToken(MUL,iterString.getIndex()));                cc = iterString.next();                continue;            case '/':                if (this.strict) {                    this.tokenList.add(new BINToken(SDIV,iterString.getIndex()));                }                else {                    this.tokenList.add(new BINToken(DIV,iterString.getIndex()));                }                cc = iterString.next();                continue;            case '&':                this.tokenList.add(new BINToken(AND,iterString.getIndex()));                cc = iterString.next();                continue;            case '%':                this.tokenList.add(new BINToken(MOD,iterString.getIndex()));                cc = iterString.next();                continue;            case '|':                this.tokenList.add(new BINToken(OR,iterString.getIndex()));                cc = iterString.next();                continue;            case '=':                this.tokenList.add(new BINToken(EQ,iterString.getIndex()));                cc = iterString.next();                continue;            case '+': {                Token lastToken = tokenList.peekLast();                if (                        lastToken.getKindOfToken() == Token.VAL ||                        lastToken.getKindOfToken() == Token.VAR ||                        lastToken.getKindOfToken() == Token.UNI ||                        lastToken.getKindOfToken() == Token.CPA                ) {                    this.tokenList.add(new BINToken(ADD,iterString.getIndex()));                    cc = iterString.next();                    continue;                }                else {                    this.tokenList.add(new UNAToken(UNAToken.PLS,iterString.getIndex()));                    cc = iterString.next();                    continue;                }            }            case '-': {                Token lastToken = tokenList.peekLast();                if (                        lastToken.getKindOfToken() == Token.VAL ||                        lastToken.getKindOfToken() == Token.VAR ||                        lastToken.getKindOfToken() == Token.UNI ||                        lastToken.getKindOfToken() == Token.CPA                ) {                    this.tokenList.add(new BINToken(SUB,iterString.getIndex()));                    cc = iterString.next();                    continue;                }                else {                    this.tokenList.add(new UNAToken(UNAToken.MIN,iterString.getIndex()));                    cc = iterString.next();                    continue;                }            }            case '<': {                int iPos = iterString.getIndex();                cc = iterString.next();                if (cc == '>') {                    this.tokenList.add(new BINToken(NE,iPos));                    cc = iterString.next();                } /* BUGFIX 9/30/2005 by Graham (further identification witheld); Add 'else'. */                else if (cc == '=') {                    this.tokenList.add(new BINToken(LE,iPos));                    cc = iterString.next();                }                else                    this.tokenList.add(new BINToken(LT,iPos));                continue;            }            case '>': {                int iPos = iterString.getIndex();                cc = iterString.next();                if (cc == '=') {                    this.tokenList.add(new BINToken(GE,iPos));                    cc = iterString.next();                }                else                    this.tokenList.add(new BINToken(GT,iPos));                continue;            }            case '!': {                int iPos = iterString.getIndex();                cc = iterString.next();                if (cc == '=') {                    this.tokenList.add(new BINToken(NE,iPos));                    cc = iterString.next();                }                else                    this.tokenList.add(new UNAToken(UNAToken.NOT,iPos));                continue;            }            default: {                throw new XExpression(iterString.getIndex(),"Unknown Symbol '"+cc+"'");            }            }            /* throw new XIllegalStatus(iterString.getIndex()); */        } /* while() */    }        private void compile() throws XExpression{        int action = 0;        int parameterCount = 0;        Deque<com.iabcinc.jmep.tokens.Token> operatorStack = new LinkedList<com.iabcinc.jmep.tokens.Token>();        Deque<Integer> parameterCountStack = new LinkedList<Integer>();        Iterator iToken;        Token topTokenOnOperatorStack;        Token token;                this.rpnStack = new LinkedList<com.iabcinc.jmep.tokens.Token>();        iToken = this.tokenList.iterator();        token = (Token)iToken.next();        operatorStack.addLast(token);        token = (Token)iToken.next();        for(;;) {            topTokenOnOperatorStack = operatorStack.peekLast();            action = arr_kDispatch[topTokenOnOperatorStack.getKindOfToken()][token.getKindOfToken()];                        if (action == D_Precedence) {                /* Two binary operators following each other */                if (                        ((BINToken)topTokenOnOperatorStack).getPrecedence() >=                            ((BINToken)token).getPrecedence()                ) {                    action = D_OSToRS;                }                else {                    action = D_TokenToOS;                }            }                        if ((action & D_IncParCount) != 0) {                /* Increase parameter count */                parameterCount++;            }                        if ((action & D_PushParCount) != 0) {                parameterCountStack.addLast(parameterCount);                parameterCount = 0;            }                        if ((action & D_PopParCount) != 0) {                ((FNCToken)topTokenOnOperatorStack).setNumberOfParameters(parameterCount);                parameterCount = parameterCountStack.removeLast();            }                        if ((action & D_OSToRS) != 0) {                /* Move token from Operator Stack to RPN Stack */                /* Should check for empty stack, if so then give internal error */                Token oMoveToken = (Token)operatorStack.removeLast();                this.rpnStack.addLast(oMoveToken);            }                        if ((action & D_TokenToOS) != 0) {                /* Move current token to Operator Stack */                operatorStack.addLast(token);                token = (Token)iToken.next();            }                        if ((action & D_TokenToRS) != 0) {                /* Move current token to RPN Stack */                this.rpnStack.addLast(token);                token = (Token)iToken.next();            }                        if ((action & D_NextToken) != 0) {                /* Current Token is next from string */                token = (Token)iToken.next();            }                        if ((action & D_PopOS) != 0) {                /* Remove token from Operator Stack */                operatorStack.removeLast();            }                        if ((action & D_PopRS) != 0) {                /* Remove token from RPN Stack */                this.rpnStack.removeLast();            }                        if ((action & D_Internal) != 0) {                this.rpnStack = null;                throw new XIllegalStatus(token.getPosition());            }                        if ((action & D_Syntax) != 0) {                this.rpnStack = null;                throw new XExpression(topTokenOnOperatorStack.getPosition(),"General Syntax error");            }                        if ((action & D_Done) != 0) {                /* Done, Result after Evaluating RPN Stack */                                // Remove last marker.                operatorStack.removeLast();                                if (!operatorStack.isEmpty() || !parameterCountStack.isEmpty()) {                    //Token oNewTopToken = (Token)oOperatorStack.getLast();                    // There should an exception been trown. */                    this.rpnStack = null;                    throw new XIllegalStatus();                }                return;            }                        if ((action & D_CheckFNCPar) != 0) {                /* Check function parameters (number and type) */            }                    }    }        private void optimize() {        /*         * TODO:         *  - implement a method to optimize the resultint RPNStack.         *    call this method 'optimize()'. This can wait, and should have         *    low priority because the MEP can work without it at         *    reasonable speed.         */    }        /**     * Evaluates the expression. This will do all necesairy late binding.     * @return the evaluated expression, wich can be Double, Integer or String.     */    public Object evaluate() throws XExpression {        Token oToken;        Deque oResultStack = new LinkedList();                for (Iterator iToken = this.rpnStack.iterator() ; iToken.hasNext() ;) {            oToken = (Token)iToken.next();            switch (oToken.getKindOfToken()) {            case Token.MRK: case Token.OPA: case Token.CMA: case Token.CPA:                /* Should never occurre */                throw new XIllegalStatus(oToken.getPosition());            case Token.FNC:                try {                    FNCToken oFNCToken = (FNCToken)oToken;                    int nParams = oFNCToken.getNumberOfParameters();                    Object oValue;                                        if (nParams != 0) {                        Object [] oValues = new Object [nParams];                        for (int iParam = 1; iParam <= nParams; iParam++)                            oValues[nParams - iParam] = oResultStack.pop();                        oValue = oFNCToken.evaluate(oValues);                    }                    else                        oValue = oFNCToken.evaluate(null);                    oResultStack.push(oValue);                }            catch (EmptyStackException x) {                /* Wrong number of arguments */                throw new XExpression(oToken.getPosition(),"Wrong number of arguments");            }            break;            case Token.UNI:                try {                    Object oValue = oResultStack.pop();                    oValue = ((UNIToken)oToken).evaluate(oValue);                    oResultStack.push(oValue);                }            catch (EmptyStackException x) {                /* Wrong number of arguments */                throw new XExpression(oToken.getPosition(),"Wrong number of arguments");            }           break;            case Token.VAR: {                Object oValue;                oValue = ((VARToken)oToken).evaluate();                oResultStack.push(oValue);                break;            }            case Token.UNA:                try {                    Object oValue = oResultStack.pop();                    oResultStack.push(((UNAToken)oToken).evaluate(oValue));                }            catch (EmptyStackException x) {                /* Wrong number of arguments */                throw new XExpression(oToken.getPosition(),"Wrong number of arguments");            }            break;            case Token.BIN:                try {                    Object oValue2 = oResultStack.pop();                    Object oValue1 = oResultStack.pop();                    Object oValue;                    oValue = ((BINToken)oToken).evaluate(oValue1,oValue2);                    oResultStack.push(oValue);                }            catch (EmptyStackException x) {                /* Wrong number of arguments */                throw new XExpression(oToken.getPosition(),"Wrong number of arguments");            }            break;            case Token.VAL:                oResultStack.push(((VALToken)oToken).getValue());            break;            }        }                try {            Object oResult = oResultStack.pop();            if (!oResultStack.isEmpty())                throw new XExpression(0,"Wrong number of arguments");            return oResult;        }        catch (EmptyStackException x) {            /* Wrong number of arguments */            throw new XExpression(0,"Wrong number of arguments");        }    }}