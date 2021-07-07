tree grammar NodeTestDSLTree;

options{
    tokenVocab=NodeTestDSL;
    ASTLabelType=CommonTree;
    output=AST;
}

@header {
    package org.kie.reteoo.test.parser;

    import org.kie.reteoo.test.dsl.*;

}

@members {
    NodeTestDSLFactory factory = new NodeTestDSLFactory();
    NodeTestCase testCase = null;

    public NodeTestCase getTestCase() {
        return testCase;
    }
}

compilation_unit
    :	 ^(VT_TEST_CASE test_case_statement import_statement* setup? teardown? test*)
    ;

test_case_statement
    : 	^(VK_TEST_CASE name=STRING)
        { testCase = factory.createTestCase( $name.text ); }
    ;

import_statement
    :	^(VK_IMPORT clazz=VT_QUALIFIED_ID)
            { testCase.addImport( $clazz.text ); }
    ;

setup
    :	^(VK_SETUP { factory.createSetup(); } step*)
    ;

teardown
    :	^(VK_TEARDOWN { factory.createTearDown(); } step*)
    ;

test	
    :	^(VK_TEST name=STRING { factory.createTest( $VK_TEST, $name ); } step*)
    ;

step
    :	^(ID p+=params+ { factory.createStep( $ID, $p ); })
    ;

params
    :	^(VT_PARAMS param_chunk+ )
    ;

param_chunk
    :	VT_CHUNK
    ;

