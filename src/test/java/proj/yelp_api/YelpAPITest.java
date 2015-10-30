package proj.yelp_api;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class YelpAPITest {
	@Test
	public void evaluatesExpression() // Connor's A6.
	{
		YelpAPI tester = new YelpAPI();
		Business[] b = tester.returnBusinesses(tester, "Happy Hour", "91210", 1);
		String name = "The Famous";
		assertEquals(name, b[0].getName());
		
	}
	
	@Test
	public void testReturnParam()	// Zenas' A6.
	{
		YelpAPI yelp = new YelpAPI();
		String[] params;
		String theHatURL = "http://www.yelp.com/biz/the-hat-brea";
		
		params = yelp.returnParam(yelp, "The Hat", "91210", "url", 1);
		assertEquals(params[0], theHatURL);
	}
	
}