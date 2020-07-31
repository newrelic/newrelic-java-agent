/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class ScopeTest {
    public boolean changeVar;

    /**
     * JAVA-859: (scoped variables) + (local scope change) == VerifyError
     * 
     * Test that changing the scope of a local variable doesn't introduce a slot collision.
     * 
     * Follow steps 1-4 to see how the bug occurs.
     */
    public int returnFive() throws Exception{
        //3. The weaver will change leet's start scope to this point.
        if(!changeVar == !changeVar){
            //4. Slot collision! At this point, bar and leet try to ocupy the same register. VerifyError is thrown.
            String bar; // 1. bar gets put in register 0
            if(!changeVar == !changeVar){
                bar = "bar";
            }
            else if(System.currentTimeMillis() > 0){
                bar = "bar2";
            }
            else{
                bar = "bar3";
            }
            System.out.println("weaved scoped true. bar: "+bar+". bar again: "+bar);
        }
        long[] leet = {1337, 34}; //2. The compiler will put leet in register 0 also since it doesn't share scope with bar.
        Weaver.callOriginal();
        if(leet[0] != 1337 || leet[1] != 34){
            throw new Exception("leet value changed");
        }
        return 6;//muhahah
    }

    /**
     * Same type of test as above, but with different types.
     */
    public int returnFour() throws Exception {
        if(!changeVar == !changeVar){
            int bar;
            if(!changeVar == !changeVar){
                bar = 30;
            }
            else if(System.currentTimeMillis() > 3){
                bar = 31;
            }
            else{
                bar = 32;
            }
            System.out.println("weaved scoped true. bar: "+bar+". bar again: "+bar);
        }
        String baz = "baz";
        Weaver.callOriginal();
        if(!"baz".equals(baz)){
            throw new Exception("baz value changed");
        }
        return 5;//muhahah
    }
    
    /**
     * Another strange scoping scenario
     */
    public int returnThree() throws Exception {
        if(System.currentTimeMillis() > 0){
            int i = 4; //one local var here to cause a slot collision
            System.out.println("i: "+i);
        }
        if(System.currentTimeMillis() > 0){
            int j = 5; //another local var here to cause another slot collision
            System.out.println("j: "+j);
        }
        String biz;//declaring biz without initializing it creates two local variables
        if(System.currentTimeMillis() > 0){
            biz = "postEpochBiz";
            //one is named biz and is scoped here
        }
        else{
            biz = "preEpochBiz";
            //this other is also named biz and is scoped here to the end of the method
        }
        Weaver.callOriginal();
        //we need to assert that changing the start scope of biz kept both biz locals in the same register
        if(!"postEpochBiz".equals(biz)){
            throw new Exception("unexpected biz");
        }
        return 4;
    }

}
