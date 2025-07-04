/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.drl.parser.lang;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.drools.drl.ast.descr.AnnotatedBaseDescr;
import org.drools.drl.ast.descr.AttributeDescr;
import org.drools.drl.ast.descr.BaseDescr;
import org.drools.drl.ast.descr.RelationalExprDescr;
import org.drools.drl.ast.descr.RuleDescr;
import org.drools.drl.ast.dsl.AbstractClassTypeDeclarationBuilder;
import org.drools.drl.ast.dsl.AccumulateDescrBuilder;
import org.drools.drl.ast.dsl.AccumulateImportDescrBuilder;
import org.drools.drl.ast.dsl.AttributeDescrBuilder;
import org.drools.drl.ast.dsl.AttributeSupportBuilder;
import org.drools.drl.ast.dsl.BehaviorDescrBuilder;
import org.drools.drl.ast.dsl.CEDescrBuilder;
import org.drools.drl.ast.dsl.CollectDescrBuilder;
import org.drools.drl.ast.dsl.ConditionalBranchDescrBuilder;
import org.drools.drl.ast.dsl.DeclareDescrBuilder;
import org.drools.drl.ast.dsl.DescrBuilder;
import org.drools.drl.ast.dsl.DescrFactory;
import org.drools.drl.ast.dsl.EntryPointDeclarationDescrBuilder;
import org.drools.drl.ast.dsl.EnumDeclarationDescrBuilder;
import org.drools.drl.ast.dsl.EnumLiteralDescrBuilder;
import org.drools.drl.ast.dsl.EvalDescrBuilder;
import org.drools.drl.ast.dsl.FieldDescrBuilder;
import org.drools.drl.ast.dsl.ForallDescrBuilder;
import org.drools.drl.ast.dsl.FunctionDescrBuilder;
import org.drools.drl.ast.dsl.GlobalDescrBuilder;
import org.drools.drl.ast.dsl.GroupByDescrBuilder;
import org.drools.drl.ast.dsl.ImportDescrBuilder;
import org.drools.drl.ast.dsl.NamedConsequenceDescrBuilder;
import org.drools.drl.ast.dsl.PackageDescrBuilder;
import org.drools.drl.ast.dsl.PatternContainerDescrBuilder;
import org.drools.drl.ast.dsl.PatternDescrBuilder;
import org.drools.drl.ast.dsl.QueryDescrBuilder;
import org.drools.drl.ast.dsl.RuleDescrBuilder;
import org.drools.drl.ast.dsl.TypeDeclarationDescrBuilder;
import org.drools.drl.ast.dsl.UnitDescrBuilder;
import org.drools.drl.ast.dsl.WindowDeclarationDescrBuilder;
import org.drools.drl.parser.DroolsParserException;
import org.drools.drl.parser.impl.Operator;
import org.kie.internal.builder.conf.LanguageLevelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a class to hold all the helper functions/methods used
 * by the DRL parser
 */
public class ParserHelper {
    public static final Logger LOG = LoggerFactory.getLogger(ParserHelper.class );

    public final String[]                             statementKeywords        = new String[]{
                                                                               DroolsSoftKeywords.PACKAGE,
                                                                               DroolsSoftKeywords.UNIT,
                                                                               DroolsSoftKeywords.IMPORT,
                                                                               DroolsSoftKeywords.GLOBAL,
                                                                               DroolsSoftKeywords.DECLARE,
                                                                               DroolsSoftKeywords.FUNCTION,
                                                                               DroolsSoftKeywords.RULE,
                                                                               DroolsSoftKeywords.QUERY
                                                                               };

    public List<DroolsParserException>                errors                   = new ArrayList<>();
    public LinkedList<DroolsSentence>                 editorInterface          = null;
    public boolean                                    isEditorInterfaceEnabled = false;
    private Deque<Map<DroolsParaphraseTypes, String>> paraphrases         = new ArrayDeque<>();

    // parameters from parser
    private DroolsParserExceptionFactory              errorMessageFactory      = null;
    private TokenStream                               input                    = null;
    private RecognizerSharedState                     state                    = null;

    private String                                    leftMostExpr             = null;

    // helper attribute
    private boolean                                   hasOperator              = false;

    private final LanguageLevelOption                 languageLevel;

    private static final Set<String> BUILT_IN_OPERATORS = Arrays.stream(Operator.BuiltInOperator.values())
            .map(Operator.BuiltInOperator::getSymbol)
            .collect(Collectors.toSet());

    private static int logCounterHalfConstraint = 0;
    private static int logCounterCustomOperator = 0;
    private static int logCounterInfixAnd = 0;
    private static int logCounterInfixOr = 0;
    private static int logCounterAnnotationInLhsPattern = 0;
    private static int logCounterAgendaGroup = 0;

    public ParserHelper(TokenStream input,
                        RecognizerSharedState state,
                        LanguageLevelOption languageLevel) {
        this.errorMessageFactory = new DroolsParserExceptionFactory( paraphrases, languageLevel );
        this.input = input;
        this.state = state;
        this.languageLevel = languageLevel;
    }

    public LinkedList<DroolsSentence> getEditorInterface() {
        return editorInterface;
    }

    public void setLeftMostExpr( String value ) {
        this.leftMostExpr = value;
    }

    public String getLeftMostExpr() {
        return this.leftMostExpr;
    }

    public void enableEditorInterface() {
        isEditorInterfaceEnabled = true;
    }

    public void disableEditorInterface() {
        isEditorInterfaceEnabled = false;
    }

    public void setHasOperator( boolean hasOperator ) {
        this.hasOperator = hasOperator;
    }

    public boolean getHasOperator() {
        return hasOperator;
    }

    public void beginSentence( DroolsSentenceType sentenceType ) {
        if ( isEditorInterfaceEnabled ) {
            if ( null == editorInterface ) {
                editorInterface = new LinkedList<>();
            }
            if (editorInterface.isEmpty()){
                DroolsSentence sentence = new DroolsSentence();
                sentence.setType( sentenceType );
                editorInterface.add( sentence );
            }
        }
    }

    public DroolsSentence getActiveSentence() {
        return editorInterface.getLast();
    }

    public void emit( List< ? > tokens,
                      DroolsEditorType editorType ) {
        if ( isEditorInterfaceEnabled && tokens != null ) {
            for ( Object activeObject : tokens ) {
                emit( (Token) activeObject,
                      editorType );
            }
        }
    }

    public void emit( Token token,
                      DroolsEditorType editorType ) {
        if ( isEditorInterfaceEnabled && token != null && editorType != null ) {
            ((DroolsToken) token).setEditorType( editorType );
            getActiveSentence().addContent( (DroolsToken) token );
        }
    }

    public void emit( int activeContext ) {
        if ( isEditorInterfaceEnabled ) {
            getActiveSentence().addContent( activeContext );
        }
    }

    public DroolsToken getLastTokenOnList( LinkedList< ? > list ) {
        DroolsToken lastToken = null;
        for ( Object object : list ) {
            if ( object instanceof DroolsToken ) {
                lastToken = (DroolsToken) object;
            }
        }
        return lastToken;
    }

    public String retrieveLT( int LTNumber ) {
        if ( null == input ) return null;
        if ( null == input.LT( LTNumber ) ) return null;
        if ( null == input.LT( LTNumber ).getText() ) return null;

        return input.LT( LTNumber ).getText();
    }

    public boolean validateLT( int LTNumber,
                               String text ) {
        String text2Validate = retrieveLT( LTNumber );
        return validateText( text, text2Validate );
    }

    private boolean validateText( String text, String text2Validate ) {
        return text2Validate != null && text2Validate.equals( text );
    }

    public boolean isPluggableEvaluator( int offset,
                                         boolean negated ) {
        String text2Validate = retrieveLT( offset );
        return text2Validate != null && DroolsSoftKeywords.isOperator(text2Validate, negated);
    }

    public boolean isPluggableEvaluator( boolean negated ) {
        return isPluggableEvaluator( 1,
                                     negated );
    }

    public boolean validateIdentifierKey( String text ) {
        return validateLT( 1,
                           text );
    }

    public boolean validateCEKeyword( int index ) {
        String text2Validate = retrieveLT( index );
        return validateText( text2Validate,
                             DroolsSoftKeywords.NOT ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.EXISTS ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.FORALL ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.AND ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.OR ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.COLLECT ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.FROM ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.END ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.EVAL ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.OVER ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.THEN );
    }

    public boolean validateStatement( int index ) {
        boolean ret = false;
        String text2Validate = retrieveLT( index );
        for ( String st : statementKeywords ) {
            if ( validateText( text2Validate,
                               st ) ) {
                ret = true;
                break;
            }
        }
        return ret || validateAttribute( index );
    }

    public boolean validateAttribute( int index ) {
        String text2Validate = retrieveLT( index );
        return validateText( text2Validate,
                             DroolsSoftKeywords.SALIENCE ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.ENABLED ) ||
               (validateText( text2Validate,
                              DroolsSoftKeywords.NO ) &&
                 validateLT( index + 1,
                             "-" ) &&
                 validateLT( index + 2,
                             DroolsSoftKeywords.LOOP )) ||
               (validateText( text2Validate,
                              DroolsSoftKeywords.AUTO ) &&
                 validateLT( index + 1,
                             "-" ) &&
                 validateLT( index + 2,
                             DroolsSoftKeywords.FOCUS )) ||
               (validateText( text2Validate,
                              DroolsSoftKeywords.LOCK ) &&
                 validateLT( index + 1,
                             "-" ) &&
                 validateLT( index + 2,
                             DroolsSoftKeywords.ON ) &&
                 validateLT( index + 3,
                             "-" ) &&
                 validateLT( index + 4,
                             DroolsSoftKeywords.ACTIVE )) ||
               (validateText( text2Validate,
                              DroolsSoftKeywords.AGENDA ) &&
                 validateLT( index + 1,
                             "-" ) &&
                 validateLT( index + 2,
                             DroolsSoftKeywords.GROUP )) ||
               (validateText( text2Validate,
                              DroolsSoftKeywords.ACTIVATION ) &&
                 validateLT( index + 1,
                             "-" ) &&
                 validateLT( index + 2,
                             DroolsSoftKeywords.GROUP )) ||
               (validateText( text2Validate,
                              DroolsSoftKeywords.RULEFLOW ) &&
                 validateLT( index + 1,
                             "-" ) &&
                 validateLT( index + 2,
                             DroolsSoftKeywords.GROUP )) ||
               (validateText( text2Validate,
                              DroolsSoftKeywords.DATE ) &&
                 validateLT( index + 1,
                             "-" ) &&
                 validateLT( index + 2,
                             DroolsSoftKeywords.EFFECTIVE )) ||
               (validateText( text2Validate,
                              DroolsSoftKeywords.DATE ) &&
                 validateLT( index + 1,
                             "-" ) &&
                 validateLT( index + 2,
                             DroolsSoftKeywords.EXPIRES )) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.DIALECT ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.CALENDARS ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.TIMER ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.DURATION ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.REFRACT ) ||
               validateText( text2Validate,
                             DroolsSoftKeywords.DIRECT );
    }

    public void reportError( RecognitionException ex ) {
        // if we've already reported an error and have not matched a token
        // yet successfully, don't report any errors.
        if ( state.errorRecovery ) {
            return;
        }
        state.errorRecovery = true;

        errors.add( errorMessageFactory.createDroolsException( ex ) );
    }

    public void reportError( Exception e ) {
        try {
            errors.add( errorMessageFactory.createDroolsException( e,
                                                                   input.LT( 1 ) ) );
        } catch (Exception ignored) {
            errors.add(new DroolsParserException( "Unexpected error: " + e.getMessage(), e ));
        }
    }

    /** return the raw DroolsParserException errors */
    public List<DroolsParserException> getErrors() {
        return errors;
    }

    /** Return a list of pretty strings summarising the errors */
    public List<String> getErrorMessages() {
        List<String> messages = new ArrayList<>( errors.size() );

        for ( DroolsParserException activeException : errors ) {
            messages.add( activeException.getMessage() );
        }

        return messages;
    }

    /** return true if any parser errors were accumulated */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Method that adds a paraphrase type into paraphrases stack.
     *
     * @param type
     *            paraphrase type
     */
    public void pushParaphrases( DroolsParaphraseTypes type ) {
        Map<DroolsParaphraseTypes, String> activeMap = new HashMap<>();
        activeMap.put( type,
                       "" );
        paraphrases.push( activeMap );
    }

    public Map<DroolsParaphraseTypes, String> popParaphrases() {
        return paraphrases.pop();
    }

    /**
     * Method that sets paraphrase value for a type into paraphrases stack.
     *
     * @param type
     *            paraphrase type
     * @param value
     *            paraphrase value
     */
    public void setParaphrasesValue( DroolsParaphraseTypes type,
                                     String value ) {
        paraphrases.peek().put( type,
                                value );
    }

    void setStart( DescrBuilder< ? , ? > db ) {
        setStart( db,
                  input.LT( 1 ) );
    }

    void setStart( DescrBuilder< ? , ? > db,
                   Token first ) {
        if ( db != null && first != null ) {
            db.startCharacter( ((CommonToken) first).getStartIndex() ).startLocation( first.getLine(),
                                                                                      first.getCharPositionInLine() );
        }
    }

    void setStart( BaseDescr descr,
                   Token first ) {
        if ( descr != null && first != null ) {
            descr.setLocation( first.getLine(),
                               first.getCharPositionInLine() );
            descr.setStartCharacter( ((CommonToken) first).getStartIndex() );
        }
    }

    void setEnd( BaseDescr descr ) {
        Token last = input.LT( -1 );
        if ( descr != null && last != null ) {
            int endLocation = last.getText() != null ? last.getCharPositionInLine() + last.getText().length() - 1 : last.getCharPositionInLine();
            descr.setEndCharacter( ((CommonToken) last).getStopIndex() + 1 );
            descr.setEndLocation( last.getLine(),
                                  endLocation );
        }
    }

    void setEnd( DescrBuilder< ? , ? > db ) {
        Token last = input.LT( -1 );
        if ( db != null && last != null ) {
            int endLocation = last.getText() != null ? last.getCharPositionInLine() + last.getText().length() - 1 : last.getCharPositionInLine();
            db.endCharacter( ((CommonToken) last).getStopIndex() + 1 ).endLocation( last.getLine(),
                                                                                    endLocation );
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends DescrBuilder< ? , ? >> T start( DescrBuilder< ? , ? > ctxBuilder,
                                                      Class<T> clazz,
                                                      String param ) {
        if ( state.backtracking == 0 ) {
            if ( PackageDescrBuilder.class.isAssignableFrom( clazz ) ) {
                pushParaphrases( DroolsParaphraseTypes.PACKAGE );
                beginSentence( DroolsSentenceType.PACKAGE );
                setStart( ctxBuilder );
            } else if ( ImportDescrBuilder.class.isAssignableFrom( clazz ) ) {
                ImportDescrBuilder imp;
                if ( validateLT( 2,
                                 DroolsSoftKeywords.FUNCTION ) ||
                     validateLT( 2,
                                 DroolsSoftKeywords.STATIC ) ) {
                    imp = ctxBuilder == null ?
                          DescrFactory.newPackage().newFunctionImport() :
                          ((PackageDescrBuilder) ctxBuilder).newFunctionImport();
                } else {
                    imp = ctxBuilder == null ?
                          DescrFactory.newPackage().newImport() :
                          ((PackageDescrBuilder) ctxBuilder).newImport();
                }
                pushParaphrases( DroolsParaphraseTypes.IMPORT );
                beginSentence( DroolsSentenceType.IMPORT_STATEMENT );
                setStart( imp );
                return (T) imp;
            } else if ( UnitDescrBuilder.class.isAssignableFrom( clazz ) ) {
                UnitDescrBuilder imp = ctxBuilder == null ?
                          DescrFactory.newPackage().newUnit() :
                          ((PackageDescrBuilder) ctxBuilder).newUnit();
                pushParaphrases( DroolsParaphraseTypes.UNIT );
                beginSentence( DroolsSentenceType.UNIT );
                setStart( imp );
                return (T) imp;
            } else if ( AccumulateImportDescrBuilder.class.isAssignableFrom( clazz ) ) {
                AccumulateImportDescrBuilder imp = ctxBuilder == null ?
                          DescrFactory.newPackage().newAccumulateImport() :
                          ((PackageDescrBuilder) ctxBuilder).newAccumulateImport();
                pushParaphrases( DroolsParaphraseTypes.ACCUMULATE_IMPORT );
                beginSentence( DroolsSentenceType.ACCUMULATE_IMPORT_STATEMENT );
                setStart( imp );
                return (T) imp;
            } else if ( GlobalDescrBuilder.class.isAssignableFrom( clazz ) ) {
                GlobalDescrBuilder global = ctxBuilder == null ?
                                            DescrFactory.newPackage().newGlobal() :
                                            ((PackageDescrBuilder) ctxBuilder).newGlobal();
                pushParaphrases( DroolsParaphraseTypes.GLOBAL );
                beginSentence( DroolsSentenceType.GLOBAL );
                setStart( global );
                return (T) global;
            } else if ( DeclareDescrBuilder.class.isAssignableFrom( clazz ) ) {
                DeclareDescrBuilder declare = ctxBuilder == null ?
                                              DescrFactory.newPackage().newDeclare() :
                                              ((PackageDescrBuilder) ctxBuilder).newDeclare();
                return (T) declare;
            } else if ( TypeDeclarationDescrBuilder.class.isAssignableFrom( clazz ) ) {
                TypeDeclarationDescrBuilder declare = ctxBuilder == null ?
                                                      DescrFactory.newPackage().newDeclare().type() :
                                                      ((DeclareDescrBuilder) ctxBuilder).type();
                pushParaphrases( DroolsParaphraseTypes.TYPE_DECLARE );
                beginSentence( DroolsSentenceType.TYPE_DECLARATION );
                setStart( declare );
                return (T) declare;
            } else if ( EnumDeclarationDescrBuilder.class.isAssignableFrom( clazz ) ) {
                EnumDeclarationDescrBuilder declare = ctxBuilder == null ?
                        DescrFactory.newPackage().newDeclare().enumerative() :
                        ((DeclareDescrBuilder) ctxBuilder).enumerative();
                pushParaphrases( DroolsParaphraseTypes.ENUM_DECLARE );
                beginSentence( DroolsSentenceType.ENUM_DECLARATION );
                setStart( declare );
                return (T) declare;
            }else if ( EntryPointDeclarationDescrBuilder.class.isAssignableFrom( clazz ) ) {
                EntryPointDeclarationDescrBuilder declare = ctxBuilder == null ?
                        DescrFactory.newPackage().newDeclare().entryPoint() :
                        ((DeclareDescrBuilder) ctxBuilder).entryPoint();
                pushParaphrases( DroolsParaphraseTypes.ENTRYPOINT_DECLARE );
                beginSentence( DroolsSentenceType.ENTRYPOINT_DECLARATION );
                setStart( declare );
                return (T) declare;
            } else if ( WindowDeclarationDescrBuilder.class.isAssignableFrom( clazz ) ) {
                WindowDeclarationDescrBuilder declare = ctxBuilder == null ?
                                                            DescrFactory.newPackage().newDeclare().window() :
                                                            ((DeclareDescrBuilder) ctxBuilder).window();
                pushParaphrases( DroolsParaphraseTypes.WINDOW_DECLARE );
                beginSentence( DroolsSentenceType.WINDOW_DECLARATION );
                setStart( declare );
                return (T) declare;
            } else if ( FieldDescrBuilder.class.isAssignableFrom( clazz ) ) {
                FieldDescrBuilder field = ((AbstractClassTypeDeclarationBuilder) ctxBuilder).newField( param );
                setStart( field );
                return (T) field;
            } else if ( EnumLiteralDescrBuilder.class.isAssignableFrom( clazz ) ) {
                EnumLiteralDescrBuilder literal = ((EnumDeclarationDescrBuilder) ctxBuilder).newEnumLiteral( param );
                setStart( literal );
                return (T) literal;
            } else if ( FunctionDescrBuilder.class.isAssignableFrom( clazz ) ) {
                FunctionDescrBuilder function;
                if ( ctxBuilder == null ) {
                    function = DescrFactory.newPackage().newFunction();
                } else {
                    PackageDescrBuilder pkg = (PackageDescrBuilder) ctxBuilder;
                    function = pkg.newFunction().namespace( pkg.getDescr().getName() );
                    AttributeDescr attribute = pkg.getDescr().getAttribute( "dialect" );
                    if ( attribute != null ) {
                        function.dialect( attribute.getValue() );
                    }
                }
                pushParaphrases( DroolsParaphraseTypes.FUNCTION );
                beginSentence( DroolsSentenceType.FUNCTION );
                setStart( function );
                return (T) function;
            } else if ( RuleDescrBuilder.class.isAssignableFrom( clazz ) ) {
                RuleDescrBuilder rule = ctxBuilder == null ?
                                        DescrFactory.newPackage().newRule() :
                                        ((PackageDescrBuilder) ctxBuilder).newRule();
                pushParaphrases( DroolsParaphraseTypes.RULE );
                beginSentence( DroolsSentenceType.RULE );
                setStart( rule );
                return (T) rule;
            } else if ( QueryDescrBuilder.class.isAssignableFrom( clazz ) ) {
                QueryDescrBuilder query = ctxBuilder == null ?
                                        DescrFactory.newPackage().newQuery() :
                                        ((PackageDescrBuilder) ctxBuilder).newQuery();
                pushParaphrases( DroolsParaphraseTypes.QUERY );
                beginSentence( DroolsSentenceType.QUERY );
                setStart( query );
                return (T) query;
            } else if ( AttributeDescrBuilder.class.isAssignableFrom( clazz ) ) {
                AttributeDescrBuilder< ? > attribute = ((AttributeSupportBuilder< ? >) ctxBuilder).attribute(param);
                setStart( attribute );
                return (T) attribute;
            } else if ( EvalDescrBuilder.class.isAssignableFrom( clazz ) ) {
                EvalDescrBuilder< ? > eval = ((CEDescrBuilder< ? , ? >) ctxBuilder).eval();
                pushParaphrases( DroolsParaphraseTypes.EVAL );
                beginSentence( DroolsSentenceType.EVAL );
                setStart( eval );
                return (T) eval;
            } else if ( ForallDescrBuilder.class.isAssignableFrom( clazz ) ) {
                ForallDescrBuilder< ? > forall = ((CEDescrBuilder< ? , ? >) ctxBuilder).forall();
                setStart( forall );
                return (T) forall;
            } else if ( CEDescrBuilder.class.isAssignableFrom( clazz ) ) {
                setStart( ctxBuilder );
                return (T) ctxBuilder;
            } else if ( PatternDescrBuilder.class.isAssignableFrom( clazz ) ) {
                PatternDescrBuilder< ? > pattern = ((PatternContainerDescrBuilder< ? , ? >) ctxBuilder).pattern();
                pushParaphrases( DroolsParaphraseTypes.PATTERN );
                setStart( pattern );
                return (T) pattern;
            } else if ( CollectDescrBuilder.class.isAssignableFrom( clazz ) ) {
                CollectDescrBuilder< ? > collect = ((PatternDescrBuilder< ? >) ctxBuilder).from().collect();
                setStart( collect );
                return (T) collect;
            } else if ( GroupByDescrBuilder.class.isAssignableFrom(clazz) ) {
                // GroupBy extends Accumulate and thus need to be before it
                GroupByDescrBuilder< ? > groupBy = ((PatternDescrBuilder< ? >) ctxBuilder).from().groupBy();
                setStart( groupBy );
                return (T) groupBy;
            } else if ( AccumulateDescrBuilder.class.isAssignableFrom( clazz ) ) {
                AccumulateDescrBuilder< ? > accumulate = ((PatternDescrBuilder< ? >) ctxBuilder).from().accumulate();
                setStart( accumulate );
                return (T) accumulate;
            } else if ( BehaviorDescrBuilder.class.isAssignableFrom( clazz ) ) {
                BehaviorDescrBuilder< ? > behavior = ((PatternDescrBuilder< ? >) ctxBuilder).behavior();
                setStart( behavior );
                return (T) behavior;
            } else if ( NamedConsequenceDescrBuilder.class.isAssignableFrom( clazz ) ) {
                NamedConsequenceDescrBuilder< ? > namedConsequence = ((CEDescrBuilder< ? , ? >) ctxBuilder).namedConsequence();
                setStart( namedConsequence );
                return (T) namedConsequence;
            } else if ( ConditionalBranchDescrBuilder.class.isAssignableFrom( clazz ) ) {
                ConditionalBranchDescrBuilder< ? > conditionalBranch = ((CEDescrBuilder< ? , ? >) ctxBuilder).conditionalBranch();
                setStart( conditionalBranch );
                return (T) conditionalBranch;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends DescrBuilder< ? , ? >> T end( Class<T> clazz,
                                                    DescrBuilder< ? , ? > builder ) {
        if ( state.backtracking == 0 ) {
            if ( !(FieldDescrBuilder.class.isAssignableFrom( clazz ) ||
                   AttributeDescrBuilder.class.isAssignableFrom( clazz ) ||
                   CEDescrBuilder.class.isAssignableFrom( clazz ) ||
                   CollectDescrBuilder.class.isAssignableFrom( clazz ) ||
                   AccumulateDescrBuilder.class.isAssignableFrom( clazz ) ||
                   ForallDescrBuilder.class.isAssignableFrom( clazz ) ||
                   BehaviorDescrBuilder.class.isAssignableFrom( clazz ) ||
                   ConditionalBranchDescrBuilder.class.isAssignableFrom( clazz ) ||
                   NamedConsequenceDescrBuilder.class.isAssignableFrom( clazz )) ) {
                popParaphrases();
            }

            if (RuleDescrBuilder.class.isAssignableFrom(clazz)) {
                RuleDescrBuilder ruleDescrBuilder = (RuleDescrBuilder)builder;
                ruleDescrBuilder.end().getDescr().afterRuleAdded(ruleDescrBuilder.getDescr());
            }

            setEnd( builder );
            return (T) builder;
        }
        return null;
    }

    public String[] getStatementKeywords() {
        return statementKeywords;
    }

    public static void logHalfConstraintWarn(String logicalConstraint, BaseDescr descr) {
        if (descr instanceof RelationalExprDescr relational) {
            String halfConstraintStr = logicalConstraint + " " + relational.getOperator() + " " + relational.getRight().toString();
            logHalfConstraintWarn("The use of a half constraint '" + halfConstraintStr + "' is deprecated" +
                                          " and will be removed in the future version (LanguageLevel.DRL10)." +
                                          " Please add a left operand.");
        }
    }

    public static void logHalfConstraintWarn(String message) {
        if (logCounterHalfConstraint > 10) {
            return; // suppress further warnings
        }

        logCounterHalfConstraint++;
        LOG.warn(message);
        if (logCounterHalfConstraint == 10) {
            LOG.warn("Further warnings about half constraints will be suppressed.");
        }
    }

    public static void logCustomOperatorWarn(Token token) {
        if (logCounterCustomOperator > 1) {
            return; // suppress further warnings
        }

        String operator = token.getText();
        if (BUILT_IN_OPERATORS.contains(operator)) {
            return; // built-in operator
        }
        logCounterCustomOperator++;
        LOG.warn("Custom operator will require a prefix '##' in the future version (LanguageLevel.DRL10)." +
                         " If you use LanguageLevel.DRL10, you need to change '{}' to '##{}'." +
                         " You don't need to change the rule while you use the default LanguageLevel.DRL6.", operator, operator);
    }

    public static void logInfixOrWarn(CEDescrBuilder descrBuilder) {
        if (logCounterInfixOr > 5) {
            return; // suppress further warnings
        }

        Optional<RuleDescr> ruleDescrOpt = getParentRuleDescr(descrBuilder);
        if (ruleDescrOpt.isEmpty()) {
            return;
        }

        logCounterInfixOr++;
        LOG.warn("Connecting patterns with '||' is deprecated and will be removed in the future version (LanguageLevel.DRL10)." +
                         " Please replace '||' with 'or' in rule '{}'. '||' in a constraint will remain supported.", ruleDescrOpt.get().getName());
        if (logCounterInfixOr == 5) {
            LOG.warn("Further warnings about '||' will be suppressed.");
        }
    }

    public static void logInfixAndWarn(CEDescrBuilder descrBuilder) {
        if (logCounterInfixAnd > 5) {
            return; // suppress further warnings
        }

        Optional<RuleDescr> ruleDescrOpt = getParentRuleDescr(descrBuilder);
        if (ruleDescrOpt.isEmpty()) {
            return;
        }

        logCounterInfixAnd++;
        LOG.warn("Connecting patterns with '&&' is deprecated and will be removed in the future version (LanguageLevel.DRL10)." +
                         " Please replace '&&' with 'and' in rule '{}'. '&&' in a constraint will remain supported.", ruleDescrOpt.get().getName());
        if (logCounterInfixAnd == 5) {
            LOG.warn("Further warnings about '&&' will be suppressed.");
        }
    }

    private static Optional<RuleDescr> getParentRuleDescr(DescrBuilder<?, ?> descrBuilder) {
        while (descrBuilder != null) {
            if (descrBuilder instanceof RuleDescrBuilder) {
                return Optional.of(((RuleDescrBuilder) descrBuilder).getDescr());
            } else if (descrBuilder instanceof PackageDescrBuilder) {
                return Optional.empty();
            }
            descrBuilder = descrBuilder.getParent();
        }
        return Optional.empty();
    }

    public static void logAnnotationInLhsPatternWarn(CEDescrBuilder descrBuilder) {
        if (logCounterAnnotationInLhsPattern > 5) {
            return; // suppress further warnings
        }

        Optional<RuleDescr> ruleDescrOpt = getParentRuleDescr(descrBuilder);
        if (ruleDescrOpt.isEmpty()) {
            return;
        }

        BaseDescr descr = descrBuilder.getDescr();
        if (descr instanceof AnnotatedBaseDescr annotated) {
            String annotationNames = annotated.getAnnotationNames().stream().collect(Collectors.joining(", "));
            logCounterAnnotationInLhsPattern++;
            LOG.warn("Annotation inside LHS patterns is deprecated and will be removed in the future version (LanguageLevel.DRL10)." +
                             " Found '{}' in rule '{}'. Annotation in other places will remain supported.", annotationNames, ruleDescrOpt.get().getName());
            if (logCounterAnnotationInLhsPattern == 5) {
                LOG.warn("Further warnings about Annotation inside LHS patterns will be suppressed.");
            }
        }
    }

    public static void logAgendaGroupWarn(AttributeDescr attributeDescr) {
        if (logCounterAgendaGroup > 5) {
            return; // suppress further warnings
        }

        logCounterAgendaGroup++;
        LOG.warn("'agenda-group \"{}\"' is found. 'agenda-group' is deprecated and will be dropped in the future version (LanguageLevel.DRL10)." +
                         " Please replace 'agenda-group' with 'ruleflow-group', which works as the same as 'agenda-group'.", attributeDescr.getValue());
        if (logCounterAgendaGroup == 5) {
            LOG.warn("Further warnings about 'agenda-group' will be suppressed.");
        }
    }
}
