
import kotlin.test.assertEquals
import org.junit.Test

internal class SampleTest {

	private val testSample: Sample = Sample()
	
	@Test
	fun testSum() {
		val expected = 42
		assertEquals(expected, testSample.sum(40,2))
	}
}