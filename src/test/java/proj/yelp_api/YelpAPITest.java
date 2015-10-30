package proj.yelp_api;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class YelpAPITest {
	@Test
	public void evaluatesExpression() {
		YelpAPI tester = new YelpAPI();
		Business[] b = tester.returnBusinesses(tester, "Happy Hour", "91210", "url", 1);
		String name = "The Famous";
		assertEquals(name, b[0].getName());
		
	}
}