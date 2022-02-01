package org.apache.logging.log4j.core.layout;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.log4j2.AgentUtil;

@Weave(originalName = "org.apache.logging.log4j.core.layout.StringBuilderEncoder", type = MatchType.BaseClass)
public class StringBuilderEncoder_Instrumentation {

    public void encode(final StringBuilder source, final ByteBufferDestination destination) {
        // TODO check config before appending agent metadata to log file/console output. Avoid doing if possible.
        appendAgentMetadata(source);
        Weaver.callOriginal();
    }

    private void appendAgentMetadata(StringBuilder source) {
        int breakLine = source.toString().lastIndexOf("\n");
        if (breakLine != -1) {
            source.replace(breakLine, breakLine + 1, "");
        }
        source.append(" NR-LINKING-METADATA: ").append(AgentUtil.getLinkingMetadataAsString()).append("\n");
    }

}