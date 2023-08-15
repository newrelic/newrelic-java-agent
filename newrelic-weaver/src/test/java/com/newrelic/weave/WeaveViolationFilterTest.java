package com.newrelic.weave;

import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.violation.WeaveViolationType;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WeaveViolationFilterTest {
    EnumSet<WeaveViolationType> violationTypes;

    @Before
    public void setup() {
        violationTypes = EnumSet.of(WeaveViolationType.METHOD_MISSING_REQUIRED_ANNOTATIONS, WeaveViolationType.CLASS_MISSING_REQUIRED_ANNOTATIONS);
    }


    @Test
    public void shouldIgnoreViolation_withIgnorableViolation_returnsTrue() {
        WeaveViolationFilter filter = new WeaveViolationFilter("com/nr/agent/instrumentation/SpringController_Instrumentation", violationTypes);

        WeaveViolation violation = new WeaveViolation(WeaveViolationType.METHOD_MISSING_REQUIRED_ANNOTATIONS, "com/nr/agent/instrumentation/SpringController_Instrumentation");
        assertTrue(filter.shouldIgnoreViolation(violation));

        violation = new WeaveViolation(WeaveViolationType.CLASS_MISSING_REQUIRED_ANNOTATIONS, "com/nr/agent/instrumentation/SpringController_Instrumentation");
        assertTrue(filter.shouldIgnoreViolation(violation));
    }

    @Test
    public void shouldIgnoreViolation_withNonIgnorableViolation_returnsFalse() {
        WeaveViolationFilter filter = new WeaveViolationFilter("com/nr/agent/instrumentation/SpringController_Instrumentation", violationTypes);

        WeaveViolation violation = new WeaveViolation(WeaveViolationType.FIELD_TYPE_MISMATCH, "com/nr/agent/instrumentation/SpringController_Instrumentation");
        assertFalse(filter.shouldIgnoreViolation(violation));
    }

    @Test
    public void filterViolationCollection_filtersIgnorableViolations() {
        WeaveViolationFilter filter = new WeaveViolationFilter("com/nr/agent/instrumentation/SpringController_Instrumentation", violationTypes);

        Collection<WeaveViolation> violations = new ArrayList<>();
        violations.add(new WeaveViolation(WeaveViolationType.METHOD_MISSING_REQUIRED_ANNOTATIONS, "com/nr/agent/instrumentation/SpringController_Instrumentation"));
        violations.add(new WeaveViolation(WeaveViolationType.CLASS_MISSING_REQUIRED_ANNOTATIONS, "com/nr/agent/instrumentation/SpringController_Instrumentation"));
        violations.add(new WeaveViolation(WeaveViolationType.EXPECTED_NEW_FIELD_ANNOTATION, "com/nr/agent/instrumentation/SpringController_Instrumentation"));
        violations.add(new WeaveViolation(WeaveViolationType.CLASS_ACCESS_MISMATCH, "com/nr/agent/instrumentation/SpringController_Instrumentation"));

        assertEquals(4, violations.size());

        Collection<WeaveViolation> filteredViolations = filter.filterViolationCollection(violations);
        assertEquals(2, filteredViolations.size());
        assertTrue(filteredViolations.contains(new WeaveViolation(WeaveViolationType.EXPECTED_NEW_FIELD_ANNOTATION, "com/nr/agent/instrumentation/SpringController_Instrumentation")));
        assertTrue(filteredViolations.contains(new WeaveViolation(WeaveViolationType.CLASS_ACCESS_MISMATCH, "com/nr/agent/instrumentation/SpringController_Instrumentation")));
    }

    @Test
    public void getWeavePackage_returnsCorrectValue() {
        WeaveViolationFilter filter = new WeaveViolationFilter("com/nr/agent/instrumentation/SpringController_Instrumentation", violationTypes);
        assertEquals("com/nr/agent/instrumentation/SpringController_Instrumentation", filter.getWeavePackage());
    }
}
