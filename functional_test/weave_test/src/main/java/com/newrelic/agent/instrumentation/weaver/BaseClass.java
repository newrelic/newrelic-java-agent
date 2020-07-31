/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.BaseClass)
public abstract class BaseClass {
	public int constructorCounts;
	public int recursiveCounts;
	
	@NewField
	private boolean recurse = true;
	
	public BaseClass(){
		constructorCounts++;
	}

    @Trace
    public String baseCall() {
        return "weaved " + Weaver.callOriginal();
    }
    
    public void recursiveTest(int i){
    	recursiveCounts++;
		if (recurse) {
			recurse = false;
			recursiveTest(1);
		}
		recurse = true;
    	Weaver.callOriginal();
    }

    @Trace
    public String childCall() {
        return "weaved " + Weaver.callOriginal();
    }

    @Trace
    public String justTrace(){
    	return Weaver.callOriginal();
    }
}