/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.css.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.junit.Assert;
import org.netbeans.modules.css.editor.test.TestBase;

/**
 *
 * @author marekfukala
 */
public class CssParserTest extends TestBase {

    public CssParserTest(String testName) {
        super(testName);
    }

    public static Test Xsuite(){
	TestSuite suite = new TestSuite();
        suite.addTest(new CssParserTest("testErrorInMediaRule"));
        return suite;
    }

    private SimpleNode parse(String source) throws ParseException {
        CssParser parser = new CssParser();
        parser.ReInit(new ASCII_CharStream(new StringReader(source)));
        return parser.styleSheet();
    }

    private static boolean isErrorNode(SimpleNode node) {
        return node.kind() == CssParserTreeConstants.JJTERROR_SKIPBLOCK ||
                node.kind() == CssParserTreeConstants.JJTERROR_SKIPDECL ||
                node.kind() == CssParserTreeConstants.JJTERROR_SKIP_TO_WHITESPACE;
    }

    /** returns number of error nodes underneath the node. */
    private static List<SimpleNode> getErrors(SimpleNode node) {
        final List<SimpleNode> errors = new ArrayList<SimpleNode>();
        SimpleNodeUtil.visitChildren(node, new NodeVisitor() {

            public void visit(SimpleNode node) {
                if (isErrorNode(node)) {
                    errors.add(node);
                }
            }
        });
        return errors;
    }

    private void assertNoErrors(SimpleNode node) {
        Assert.assertNotNull(node);
        List<SimpleNode> errors = getErrors(node);
        if(errors.size() > 0) {
            StringBuffer buf = new StringBuffer();
            for(SimpleNode e : errors) {
                buf.append(e.toString());
                buf.append(',');
            }
            buf.deleteCharAt(buf.length() - 1);
            assertEquals("Unexpected parse errors found: " + buf.toString(), 0, errors.size());
        }
    }

    public void testParserBasis() throws ParseException {
        SimpleNode node = parse("h1 { color: red; }");
        Assert.assertNotNull(node);
        assertNoErrors(node);
        
    }

    public void testParseComment() throws ParseException {
        assertNoErrors(parse("h1 { /* comment */ }"));
        assertNoErrors(parse("h1 { color: /* comment */ red; }"));
        assertNoErrors(parse("h1 /* c */ { /* c2 */ color: red; }"));
        assertNoErrors(parse("/* c */ h1 {  color: red; } /* c2 */"));
    }

    private SimpleNode check(String source) throws ParseException {
        SimpleNode node = parse(source);
        Assert.assertNotNull(node);
        assertNoErrors(node);
        return node;
    }

    public void testIssue183158() throws ParseException {
        String code = "div { margin-left: -49%; }";
//        dumpTokens(code);
//        dumpParseTree(code);

        check(code);
    }

    public void testIssue183601() throws ParseException {
        String code = "table tbody tr:not(.Current):hover { }";
        check(code);
    }

    public void testAtSymbol() throws ParseException {
        String code = "@a ";
        dumpTokens(code);
//        dumpParseTree(code);
        check(code);
    }

    public void testIssue182434() throws ParseException {
        //css3
        check("A[href^=\"https://\"] {}");
        check("IMG[src*=\"icon\"] {}");
        check("A[href$=\".pdf\"] {} ");

        check("div { quotes: '\"' '\"' \"'\" \"'\"; } ");
    }

    public void testMSSyntax() throws ParseException {
        check("h1 { top: expression(offsetParent.scrollTop) } ");
        check("h1 { filter:alpha(opacity=50); }");
        check("h1 { filter: progid:DXImageTransform.Microsoft.Blur(PixelRadius=5,MakeShadow=true,ShadowOpacity=0.20); }");
        check("h1 { filter: progid:DXImageTransform.Microsoft.Alpha(opacity=70) }");
        check("h1 { filter: progid:DXImageTransform.Microsoft.AlphaImageLoader(src='trans.png', sizingMethod='scale'); }");

        //IE8
        check("h1 { -ms-filter:\"progid:DXImageTransform.Microsoft.Alpha(Opacity=50)\"; }");

        check("h1 { #width: 400px }");

    }

    // @@@ represents a gap from the css perspective in reality filled with 
    // a templating language code.
    public void testParserOnTemplating() throws ParseException {
        //generated properties
        check("h1 { @@@: red; }");
        check("h1 { color: @@@; }");
        check("h1 { @@@: @@@; }");
        check("h1 { color: @@@ red @@@; }");
//        check("h1 { co@@@lor: red; }");
        check("h1 { @@@@@@: green; }");

        check("h1 { background-image: url(@@@); }");
        check("h1 { background-image: url(\"@@@\"); }");
        check("h1 { color: rgb(@@@,@@@,@@@); }");
        check("h1 { color: rgb(0,0,@@@); }");

        check("h1 { @@@ ; }");
        check("h1 { @@@; }");
        check("h1 { @@@ }");

        //selectors are generated
        check("@@@ { }");
        check("h1 @@@ h2 { }");
//        check("t@@@ble { }");

        check("table > @@@ { }");
        check("t[@@@] { }");
        check("t[x=@@@] { }");
        check("* > t[x=@@@] { }");
        check("E:lang(@@@){ }");
        check("E:@@@{ }");
        check("t[x|=@@@] { }");

//        check("#@@@ { }");//fails
//        check("E#@@@ { }");//fails

        check("@@@ + t[x=@@@] { }");

        check("h1 { @@@: rgb(@@@); }");
        check("span[hello=@@@][@@@]{}");

        check("p.@@@:first-letter {color: @@@}");

//        check("h1 {color: #@@@}");//fails
        check("media @@@{}");

//        check("media TV{ @@@ { } }");//fails
        check("@page:left{margin-left:@@@;}");
    }

    public void testTemplatingInComment() throws ParseException {
        check("EXAMPLE { /* @@@ */ }");
    }

    public void testParserRootNodeSpan() throws ParseException {
        String source = "h1 { }";
        //               0123456
        SimpleNode node = parse(source);
        Assert.assertNotNull(node);
        assertNoErrors(node);

//        Token t = node.jjtGetFirstToken();
//        while(t != null) {
//            System.out.print(t);
//            System.out.println(" ["+t.kind+"]");
//            t = t.next;
//        }

        //test the root node size - if it spans over the whole source text
        assertEquals(source.length(), node.endOffset() - node.startOffset());

    }

    public void testPropertyValueWithComment() throws ParseException {
        //fails - issue http://www.netbeans.org/issues/show_bug.cgi?id=162844
        //the problem is that the SimpleNode for property value contains also the whitespace and comment

        String code = "h3 { color: red /*.....*/ }";

//        //tokens
//        System.out.println("code='" + code + "'");
//        System.out.println("Tokens: ");
//        CssParserTokenManager tm = new PatchedCssParserTokenManager(new ASCII_CharStream(new StringReader(code)));
//        Token t;
//        while((t = tm.getNextToken()) != null && t.image.length() > 0) {
//            System.out.print("<" + t.offset + "," + t.image + "> ");
//        }
//        System.out.println(".");

        SimpleNode root = check(code);
//        System.out.println(root.dump());

        SimpleNode node = SimpleNodeUtil.query(root, "styleSheetRuleList/rule/styleRule/declaration/expr/term");
        assertNotNull(node);
        assertEquals("red", node.image(CssParserConstants.COMMENT, CssParserConstants.S));
 
    }

    public void testErrorInMediaRule() throws ParseException {
        String source = "@media page {  htm }  ";
//        dumpParseTree(source);
        SimpleNode node = parse(source);
        assertNotNull(node);

        List<SimpleNode> errors = getErrors(node);
        assertEquals(2, errors.size());

        SimpleNode error = errors.get(0);
        assertEquals(CssParserTreeConstants.JJTERROR_SKIPBLOCK, error.kind());
        error = errors.get(1);
        assertEquals(CssParserTreeConstants.JJTERROR_SKIPBLOCK, error.kind());
    }

    public void testErrorInStyleRule() throws ParseException {
        String source = "div {  htm }";
        SimpleNode node = parse(source);
        assertNotNull(node);

        List<SimpleNode> errors = getErrors(node);
        assertEquals(2, errors.size());

        SimpleNode error = errors.get(0);
        assertEquals(CssParserTreeConstants.JJTERROR_SKIPDECL, error.kind());
        error = errors.get(1);
        assertEquals(CssParserTreeConstants.JJTERROR_SKIPBLOCK, error.kind());
    }

    public void testErrorInDeclaration() throws ParseException {
        String source = "div {  color: ; azimuth: center; }";
        SimpleNode node = parse(source);
        assertNotNull(node);

        List<SimpleNode> errors = getErrors(node);


        assertEquals(2, errors.size());

        SimpleNode error = errors.get(0);
        assertEquals(CssParserTreeConstants.JJTERROR_SKIPDECL, error.kind());
        error = errors.get(1);
        assertEquals(CssParserTreeConstants.JJTERROR_SKIPBLOCK, error.kind());
    }

    public void testErrorInDeclarationInMediaRule() throws ParseException {
        String source = "@media page { div { color: } } ";
        SimpleNode node = parse(source);
        assertNotNull(node);

        List<SimpleNode> errors = getErrors(node);


        assertEquals(2, errors.size());

        SimpleNode error = errors.get(0);
        assertEquals(CssParserTreeConstants.JJTERROR_SKIPDECL, error.kind());
        error = errors.get(1);
        assertEquals(CssParserTreeConstants.JJTERROR_SKIPBLOCK, error.kind());
    }


    private void dumpTokens(String source) {
    CssParserTokenManager tm = new CssParserTokenManager(new ASCII_CharStream(new StringReader(source)));
        org.netbeans.modules.css.parser.Token token = null;
        do {
            token = tm.getNextToken();
            System.out.println(token + "; kind = " + token.kind + " (" + CssParserConstants.tokenImage[token.kind] + ")");
            if(token == null) {
                break;
            }
            if(token.kind == CssParserConstants.EOF) {
                break;
            }
        } while(true);
    }

    private void dumpParseTree(String source) throws ParseException {
        SimpleNode node = parse(source);
        assertNotNull(node);
        System.out.println(node.dump());
    }
}

