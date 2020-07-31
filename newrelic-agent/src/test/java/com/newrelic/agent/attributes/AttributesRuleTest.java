/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import org.junit.Assert;
import org.junit.Test;

public class AttributesRuleTest {

    @Test
    public void testMatchesExact() {
        AttributesNode rule = new AttributesNode("name", true, "Test", false);
        Assert.assertTrue(rule.matches("name"));

        Assert.assertFalse(rule.matches("nam"));
        Assert.assertFalse(rule.matches("name1"));
        Assert.assertFalse(rule.matches("name2"));
        Assert.assertFalse(rule.matches("na"));

        rule = new AttributesNode("bam", true, "Test", false);
        Assert.assertTrue(rule.matches("bam"));

        Assert.assertFalse(rule.matches("bame"));
        Assert.assertFalse(rule.matches("ba"));
        Assert.assertFalse(rule.matches("name"));

        rule = new AttributesNode("", true, "Test", false);
        Assert.assertTrue(rule.matches(""));

        Assert.assertFalse(rule.matches("n"));
        Assert.assertFalse(rule.matches("*"));
        Assert.assertFalse(rule.matches("."));

    }

    @Test
    public void testMatchesEndWildcard() {
        AttributesNode rule = new AttributesNode("name*", true, "Test", false);
        Assert.assertTrue(rule.matches("name"));
        Assert.assertTrue(rule.matches("name1"));
        Assert.assertTrue(rule.matches("name."));
        Assert.assertTrue(rule.matches("name*"));
        Assert.assertTrue(rule.matches("name.second"));

        Assert.assertFalse(rule.matches("nam"));
        Assert.assertFalse(rule.matches("1name"));
        Assert.assertFalse(rule.matches(""));

        rule = new AttributesNode("*", true, "Test", false);
        Assert.assertTrue(rule.matches("name"));
        Assert.assertTrue(rule.matches("."));
        Assert.assertTrue(rule.matches(""));
        Assert.assertTrue(rule.matches("!$%&"));

        rule = new AttributesNode("1*2*", true, "Test", false);
        Assert.assertTrue(rule.matches("1*2"));
        Assert.assertTrue(rule.matches("1*2345."));
        Assert.assertTrue(rule.matches("1*2..."));

        Assert.assertFalse(rule.matches("1.2*"));
        Assert.assertFalse(rule.matches("1324"));
        Assert.assertFalse(rule.matches("102"));
        Assert.assertFalse(rule.matches("!$%&"));
        Assert.assertFalse(rule.matches("nam"));
        Assert.assertFalse(rule.matches("1name"));
    }

    // Tests going from a alone, to a-> b, to a-> c -> b
    @Test
    public void testAddNodeOneReplace() {
        AttributesNode root = new AttributesNode("*", true, "Test", false);
        Assert.assertNull(root.getParent());
        Assert.assertEquals(0, root.getChildren().size());

        // go from a to a -> b
        AttributesNode rootChild1 = new AttributesNode("name*", true, "Test", false);

        root.addNode(rootChild1);
        Assert.assertNull(root.getParent());
        Assert.assertEquals(1, root.getChildren().size());
        Assert.assertTrue(root.getChildren().contains(rootChild1));
        Assert.assertEquals(root, rootChild1.getParent());
        Assert.assertEquals(0, rootChild1.getChildren().size());

        // go from a -> b to a -> c -> b
        AttributesNode replacementRootChild1 = new AttributesNode("na*", true, "Test", false);
        root.addNode(replacementRootChild1);

        Assert.assertNull(root.getParent());
        Assert.assertEquals(1, root.getChildren().size());
        Assert.assertTrue(root.getChildren().contains(replacementRootChild1));
        Assert.assertFalse(root.getChildren().contains(rootChild1));
        Assert.assertEquals(root, replacementRootChild1.getParent());
        Assert.assertEquals(1, replacementRootChild1.getChildren().size());
        Assert.assertTrue(replacementRootChild1.getChildren().contains(rootChild1));
        Assert.assertEquals(replacementRootChild1, rootChild1.getParent());
        Assert.assertEquals(0, rootChild1.getChildren().size());
    }

    // Tests going from a alone, to a-> b, to a-> -> b -> c
    @Test
    public void testAddNodeTwoChildren() {
        AttributesNode root = new AttributesNode("*", true, "Test", false);
        Assert.assertNull(root.getParent());
        Assert.assertEquals(0, root.getChildren().size());

        // go from a to a -> b
        AttributesNode rootChild1 = new AttributesNode("name*", true, "Test", false);

        root.addNode(rootChild1);
        Assert.assertNull(root.getParent());
        Assert.assertEquals(1, root.getChildren().size());
        Assert.assertTrue(root.getChildren().contains(rootChild1));
        Assert.assertEquals(root, rootChild1.getParent());
        Assert.assertEquals(0, rootChild1.getChildren().size());

        // go from a -> b to a -> -> b -> c
        AttributesNode replacementRootChild1 = new AttributesNode("name1*", true, "Test", false);
        root.addNode(replacementRootChild1);

        Assert.assertNull(root.getParent());
        Assert.assertEquals(1, root.getChildren().size());
        Assert.assertTrue(root.getChildren().contains(rootChild1));
        Assert.assertFalse(root.getChildren().contains(replacementRootChild1));
        Assert.assertEquals(root, rootChild1.getParent());
        Assert.assertEquals(1, rootChild1.getChildren().size());
        Assert.assertTrue(rootChild1.getChildren().contains(replacementRootChild1));
        Assert.assertEquals(rootChild1, replacementRootChild1.getParent());
        Assert.assertEquals(0, replacementRootChild1.getChildren().size());
    }

    // Tests going from a alone, to a-> b*, to a-> -> b* -> b
    @Test
    public void testAddNodeNameAfterNameStar() {
        AttributesNode root = new AttributesNode("*", true, "Test", false);
        Assert.assertNull(root.getParent());
        Assert.assertEquals(0, root.getChildren().size());

        // go from a to a -> b*
        AttributesNode rootChild1 = new AttributesNode("name*", true, "Test", false);

        root.addNode(rootChild1);
        Assert.assertNull(root.getParent());
        Assert.assertEquals(1, root.getChildren().size());
        Assert.assertTrue(root.getChildren().contains(rootChild1));
        Assert.assertEquals(root, rootChild1.getParent());
        Assert.assertEquals(0, rootChild1.getChildren().size());

        // go from a -> b* to a -> -> b* -> b
        AttributesNode replacementRootChild1 = new AttributesNode("name", true, "Test", false);
        root.addNode(replacementRootChild1);

        Assert.assertNull(root.getParent());
        Assert.assertEquals(1, root.getChildren().size());
        Assert.assertTrue(root.getChildren().contains(rootChild1));
        Assert.assertFalse(root.getChildren().contains(replacementRootChild1));
        Assert.assertEquals(root, rootChild1.getParent());
        Assert.assertEquals(1, rootChild1.getChildren().size());
        Assert.assertTrue(rootChild1.getChildren().contains(replacementRootChild1));
        Assert.assertEquals(rootChild1, replacementRootChild1.getParent());
        Assert.assertEquals(0, replacementRootChild1.getChildren().size());
    }

    @Test
    public void testAddNode() {
        // a
        AttributesNode root = new AttributesNode("*", true, "Test", false);
        Assert.assertNull(root.getParent());
        Assert.assertEquals(0, root.getChildren().size());

        // a -> b*
        AttributesNode bStar = new AttributesNode("name*", true, "Test", false);
        root.addNode(bStar);

        // a -> b* , c*
        AttributesNode cStar = new AttributesNode("nab*", true, "Test", false);
        root.addNode(cStar);

        // a -> b* , c* , d
        AttributesNode d = new AttributesNode("rain", true, "Test", false);
        root.addNode(d);

        // a -> b* , c* , dStar -> d
        AttributesNode dStar = new AttributesNode("rain*", true, "Test", false);
        root.addNode(dStar);

        // ---- a ----
        // b* -- c* -- d*
        // e* ---------- d
        // or in other words a -> b* -> e* , c* , d* -> d
        AttributesNode eStar = new AttributesNode("namer*", true, "Test", false);
        root.addNode(eStar);

        // ---- a ----
        // b* -- c* -- d*
        // e* --------- d
        // f
        // or in other words a -> b* -> e* -> f , c* , d* -> d
        AttributesNode f = new AttributesNode("namer.gamer", true, "Test", false);
        root.addNode(f);

        // ---- a ----
        // - bc* ----- d*
        // b* -- c* -- d
        // e* ---------
        // f
        // or in other words a -> bc* -> b* -> e* -> f , c* , d* -> d
        AttributesNode bcStar = new AttributesNode("na*", true, "Test", false);
        root.addNode(bcStar);

        // ---- a ----
        // - bc* ---- g*
        // b* -- c* -- d*
        // e* --------- d
        // f ---
        // or in other words a -> bc* -> b* -> e* -> f , c* , g* -> d* -> d
        AttributesNode gStar = new AttributesNode("ra*", true, "Test", false);
        root.addNode(gStar);

        // ---- a ----
        // - bc* ------ g*
        // -- b* --- c* -- d*
        // e* - h---------- d
        // f ---
        // or in other words a -> bc* -> b* -> h, e* -> f , c* , g* -> d* -> d
        AttributesNode h = new AttributesNode("named", true, "Test", false);
        root.addNode(h);

        root.printTrie();
        // verify a -> bc* and g*
        Assert.assertNull(root.getParent());
        Assert.assertEquals(2, root.getChildren().size());
        Assert.assertTrue(root.getChildren().contains(bcStar));
        Assert.assertTrue(root.getChildren().contains(gStar));

        // verify bc*
        Assert.assertEquals(root, bcStar.getParent());
        Assert.assertEquals(2, bcStar.getChildren().size());
        Assert.assertTrue(bcStar.getChildren().contains(bStar));
        Assert.assertTrue(bcStar.getChildren().contains(cStar));

        // verify g*
        Assert.assertEquals(root, gStar.getParent());
        Assert.assertEquals(1, gStar.getChildren().size());
        Assert.assertTrue(gStar.getChildren().contains(dStar));

        // verify b*
        Assert.assertEquals(bcStar, bStar.getParent());
        Assert.assertEquals(2, bStar.getChildren().size());
        Assert.assertTrue(bStar.getChildren().contains(eStar));
        Assert.assertTrue(bStar.getChildren().contains(h));

        // verify c*
        Assert.assertEquals(bcStar, cStar.getParent());
        Assert.assertEquals(0, cStar.getChildren().size());

        // verify d*
        Assert.assertEquals(gStar, dStar.getParent());
        Assert.assertEquals(1, dStar.getChildren().size());
        Assert.assertTrue(dStar.getChildren().contains(d));

        // verify e*
        Assert.assertEquals(bStar, eStar.getParent());
        Assert.assertEquals(1, eStar.getChildren().size());
        Assert.assertTrue(eStar.getChildren().contains(f));

        // verify f
        Assert.assertEquals(bStar, h.getParent());
        Assert.assertEquals(0, h.getChildren().size());

        // verify d
        Assert.assertEquals(dStar, d.getParent());
        Assert.assertEquals(0, d.getChildren().size());

        // verify f
        Assert.assertEquals(eStar, f.getParent());
        Assert.assertEquals(0, f.getChildren().size());
    }

    // tree is root-> b -> c -> d
    @Test
    public void testApplyRulesDepth() {

        AttributesNode root = new AttributesNode("*", true, "Test", false);
        AttributesNode b = new AttributesNode("ba*", false, "Test", false);
        AttributesNode c = new AttributesNode("bad*", true, "Test", false);
        AttributesNode d = new AttributesNode("bad", false, "Test", false);
        root.addNode(b);
        root.addNode(c);
        root.addNode(d);

        runAndVerifyOutput(root, "robot", true);
        runAndVerifyOutput(root, "bat", false);
        runAndVerifyOutput(root, "baddd", true);
        runAndVerifyOutput(root, "bad", false);
    }

    // tree is root-> b, c, d
    @Test
    public void testApplyRulesWidth() {

        AttributesNode root = new AttributesNode("*", false, "Test", false);
        AttributesNode b = new AttributesNode("ba*", true, "Test", false);
        AttributesNode c = new AttributesNode("ra*", false, "Test", false);
        AttributesNode d = new AttributesNode("mad", true, "Test", false);
        root.addNode(b);
        root.addNode(c);
        root.addNode(d);

        runAndVerifyOutput(root, "robot", false);
        runAndVerifyOutput(root, "bat", true);
        runAndVerifyOutput(root, "rat", false);
        runAndVerifyOutput(root, "mad", true);
    }

    // --------- root -----------
    // ---- b ------ c -------- d
    // - ba - bb --- cc ---dd -de- df
    // - baa ----
    @Test
    public void testApplyRulesComplex() {

        AttributesNode root = new AttributesNode("*", true, "Test", false);
        AttributesNode b = new AttributesNode("b*", false, "Test", false);
        AttributesNode ba = new AttributesNode("ba*", true, "Test", false);
        AttributesNode baa = new AttributesNode("baa", false, "Test", false);
        AttributesNode bb = new AttributesNode("bb*", false, "Test", false);
        AttributesNode c = new AttributesNode("c*", true, "Test", false);
        AttributesNode cc = new AttributesNode("cc*", false, "Test", false);
        AttributesNode d = new AttributesNode("d*", false, "Test", false);
        AttributesNode dd = new AttributesNode("dd*", true, "Test", false);
        AttributesNode de = new AttributesNode("de*", false, "Test", false);
        AttributesNode df = new AttributesNode("df", true, "Test", false);

        root.addNode(b);
        root.addNode(baa);
        root.addNode(ba);
        root.addNode(bb);
        root.addNode(cc);
        root.addNode(c);
        root.addNode(d);
        root.addNode(dd);
        root.addNode(de);
        root.addNode(df);

        // --------- root -----------
        // ---- b ------ c -------- d
        // - ba - bb --- cc ---dd -de- df
        // - baa ----

        // hits root
        runAndVerifyOutput(root, "robot", true);
        // hits root and b
        runAndVerifyOutput(root, "brrr", false);
        // hits root -> b -> ba
        runAndVerifyOutput(root, "bart", true);
        // hits root -> b -> ba -> baa
        runAndVerifyOutput(root, "baa", false);
        // hits root -> b -> bb
        runAndVerifyOutput(root, "bbbbb", false);
        // hits root -> c
        runAndVerifyOutput(root, "cabin", true);
        // hits root -> c -> cc
        runAndVerifyOutput(root, "cccccc", false);
        // hits root -> d
        runAndVerifyOutput(root, "dragon", false);
        // hits root -> d -> dd
        runAndVerifyOutput(root, "dddddd", true);
        // hits root -> d -> de
        runAndVerifyOutput(root, "dead", false);
        // hits root -> d -> df
        runAndVerifyOutput(root, "df", true);

    }

    private void runAndVerifyOutput(AttributesNode root, String input, boolean expected) {
        Assert.assertEquals("unexpected result for include/exclude for input: " + input, expected,
                root.applyRules(input));
    }

    @Test
    public void testMergeIncludeExcludesBasic1() {
        AttributesNode root1 = new AttributesNode("*", true, "Test", false);
        AttributesNode root2 = new AttributesNode("*", true, "Test", false);

        root1.addNode(root2);

        runAndVerifyOutput(root1, "haha", true);
    }

    @Test
    public void testMergeIncludeExcludesBasic2() {
        AttributesNode root1 = new AttributesNode("*", true, "Test", false);
        AttributesNode root2 = new AttributesNode("*", false, "Test", false);

        root1.addNode(root2);

        runAndVerifyOutput(root1, "haha", false);
    }

    @Test
    public void testMergeIncludeExcludesBasic3() {
        AttributesNode root1 = new AttributesNode("*", false, "Test", false);
        AttributesNode root2 = new AttributesNode("*", true, "Test", false);

        root1.addNode(root2);

        runAndVerifyOutput(root1, "haha", false);
    }

    // root -> b(d) , c
    @Test
    public void testMergeIncludeExcludes() {
        // apply false onto true
        AttributesNode root1 = new AttributesNode("*", true, "Test", false);
        AttributesNode b = new AttributesNode("b*", true, "Test", false);
        AttributesNode c = new AttributesNode("c*", false, "Test", false);
        AttributesNode d = new AttributesNode("b*", false, "Test", false);

        root1.addNode(b);
        root1.addNode(c);
        root1.addNode(d);

        runAndVerifyOutput(root1, "baa", false);

        // now apply true onto false
        root1 = new AttributesNode("*", true, "Test", false);
        b = new AttributesNode("b*", false, "Test", false);
        c = new AttributesNode("c*", false, "Test", false);
        d = new AttributesNode("b*", true, "Test", false);

        root1.addNode(b);
        root1.addNode(c);
        root1.addNode(d);

        runAndVerifyOutput(root1, "baa", false);
    }
}
