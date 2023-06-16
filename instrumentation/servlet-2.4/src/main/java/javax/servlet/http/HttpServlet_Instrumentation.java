package javax.servlet.http;


import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

import javax.servlet.ServletException;
import java.io.IOException;


@Weave(type = MatchType.BaseClass, originalName = "javax.servlet.http.HttpServlet")
public abstract class HttpServlet_Instrumentation {


    @Trace
    protected abstract void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;


    @Trace
    protected abstract void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;


    @Trace
    protected abstract void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;


    @Trace
    protected abstract void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;


}