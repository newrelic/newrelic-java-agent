/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package jakarta.servlet.jsp.tagext;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.nr.agent.instrumentation.jsp4.JspContextWrapper;
import com.nr.agent.instrumentation.jsp4.JspUtils;
import com.nr.agent.instrumentation.jsp4.JspWriterWrapper;

import jakarta.servlet.jsp.JspContext;

@Weave(type = MatchType.ExactClass, originalName = "jakarta.servlet.jsp.tagext.SimpleTagSupport")
public class SimpleTagSupport_Instrumentation {
    private JspContext jspContext;

    public void setJspContext(JspContext jspContext) {
        if (JspUtils.isTagLibInstrumentationEnabled()) {
            JspWriterWrapper wrapperWriter = new JspWriterWrapper(jspContext.getOut());
            this.jspContext = new JspContextWrapper(jspContext, wrapperWriter);
        } else {
            this.jspContext = jspContext;
        }
    }
}
