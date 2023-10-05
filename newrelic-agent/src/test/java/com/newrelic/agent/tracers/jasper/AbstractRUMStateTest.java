package com.newrelic.agent.tracers.jasper;

import com.newrelic.agent.Transaction;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AbstractRUMStateTest {
    @Test
    public void writeHeader_writesHeaderToJsp() throws Exception {
        TestRUMState testRUMState = new TestRUMState();
        Transaction mockTxn = mock(Transaction.class);
        GenerateVisitor mockGenVisitor = mock(GenerateVisitor.class);
        TemplateText mockTemplateText = mock(TemplateText.class);

        try (MockedStatic<GeneratorVisitTracerFactory> mockFactory = mockStatic(GeneratorVisitTracerFactory.class)) {
            mockFactory.when(() -> GeneratorVisitTracerFactory.getPage(mockTxn)).thenReturn("jsp");
            testRUMState.writeHeader(mockTxn, mockGenVisitor, mockTemplateText, "text1text2", 5);
            verify(mockTemplateText, times(3)).setText(anyString());
            verify(mockGenVisitor, times(2)).visit(mockTemplateText);
        }
    }

    @Test
    public void writeFooter_writesHeaderToJsp() throws Exception {
        TestRUMState testRUMState = new TestRUMState();
        Transaction mockTxn = mock(Transaction.class);
        GenerateVisitor mockGenVisitor = mock(GenerateVisitor.class);
        TemplateText mockTemplateText = mock(TemplateText.class);

        try (MockedStatic<GeneratorVisitTracerFactory> mockFactory = mockStatic(GeneratorVisitTracerFactory.class)) {
            mockFactory.when(() -> GeneratorVisitTracerFactory.getPage(mockTxn)).thenReturn("jsp");
            testRUMState.writeFooter(mockTxn, mockGenVisitor, mockTemplateText, "text1text2", 5);
            verify(mockTemplateText, times(3)).setText(anyString());
            verify(mockGenVisitor, times(2)).visit(mockTemplateText);
        }
    }

    private static class TestRUMState extends AbstractRUMState {

        @Override
        public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text) throws Exception {
            return null;
        }
    }
}
