/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package jakarta.servlet.jsp.tagext;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.nr.agent.instrumentation.jsp4.JspUtils;
import com.nr.agent.instrumentation.jsp4.JspWriterWrapper;
import com.nr.agent.instrumentation.jsp4.PageContextWrapper;

import jakarta.servlet.jsp.PageContext;

@Weave(type = MatchType.ExactClass, originalName = "jakarta.servlet.jsp.tagext.TagSupport")
public class TagSupport_Instrumentation {
    protected transient PageContext pageContext;

    public void setPageContext(PageContext pageContext) {
        if (JspUtils.isTagLibInstrumentationEnabled()) {
            JspWriterWrapper wrapperWriter = new JspWriterWrapper(pageContext.getOut());
            this.pageContext = new PageContextWrapper(pageContext, wrapperWriter);
        } else {
            this.pageContext = pageContext;
        }
    }
}
