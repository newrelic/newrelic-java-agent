package org.springframework.web.util;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.SpringPlaceholderConfig;

import javax.servlet.http.HttpServletRequest;

@Weave
public class UrlPathHelper {

    public UrlPathHelper(){}

    public String getLookupPathForRequest(HttpServletRequest request) {
        String result = Weaver.callOriginal();
        Transaction tx = NewRelic.getAgent().getTransaction();

        if(SpringPlaceholderConfig.springPlaceholderValue && result != null && tx != null){
            String methodName = (request.getMethod() != null) ? " ("+request.getMethod()+")" : "";
            tx.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "SpringController",
                    result + methodName);
        }

        return result;
    }
}