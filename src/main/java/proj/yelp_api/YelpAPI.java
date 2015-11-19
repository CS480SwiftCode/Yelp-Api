package proj.yelp_api;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public class YelpAPI {

	private static final String API_HOST = "api.yelp.com";
	// private static final String DEFAULT_TERM = "Happy Hour";
	private static final int SEARCH_LIMIT = 3;
	private static final String SEARCH_PATH = "/v2/search";
	private static final String BUSINESS_PATH = "/v2/business";

	private static final String CONSUMER_KEY;
	private static final String CONSUMER_SECRET;
	private static final String TOKEN;
	private static final String TOKEN_SECRET;

	OAuthService service;
	Token accessToken;

	static {
		try {
			File keys = new File("key.cfg");
			Properties prop = new Properties();
			prop.load(new FileReader(keys));
			CONSUMER_KEY = prop.getProperty("CKEY");
			CONSUMER_SECRET = prop.getProperty("CSECRET");
			TOKEN = prop.getProperty("TOKEN");
			TOKEN_SECRET = prop.getProperty("TOKENSECRET");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Init failed. Requires key.cfg.");
		}
	}

	public YelpAPI() {
		this.service = new ServiceBuilder().provider(TwoStepOAuth.class).apiKey(CONSUMER_KEY).apiSecret(CONSUMER_SECRET)
				.build();
		this.accessToken = new Token(TOKEN, TOKEN_SECRET);
	}

	public YelpAPI(String consumerKey, String consumerSecret, String token, String tokenSecret) {
		this.service = new ServiceBuilder().provider(TwoStepOAuth.class).apiKey(consumerKey).apiSecret(consumerSecret)
				.build();
		this.accessToken = new Token(token, tokenSecret);
	}

	public String searchForBusinessesByLocation(String term, String location) {
		OAuthRequest request = createOAuthRequest(SEARCH_PATH);
		request.addQuerystringParameter("term", term);
		request.addQuerystringParameter("location", location);
		request.addQuerystringParameter("limit", String.valueOf(SEARCH_LIMIT));
		return sendRequestAndGetResponse(request);
	}

	public String searchForBusinessesByLocation(String term, String location, int limit) {
		OAuthRequest request = createOAuthRequest(SEARCH_PATH);
		request.addQuerystringParameter("term", term);
		request.addQuerystringParameter("location", location);
		request.addQuerystringParameter("limit", String.valueOf(limit));
		return sendRequestAndGetResponse(request);
	}

	public String searchForBusinessesByLocation(String term, String location, int limit, int radius) {
		OAuthRequest request = createOAuthRequest(SEARCH_PATH);
		request.addQuerystringParameter("term", term);
		request.addQuerystringParameter("location", location);
		request.addQuerystringParameter("limit", String.valueOf(limit));
		request.addQuerystringParameter("radius_filter", String.valueOf(radius));
		return sendRequestAndGetResponse(request);
	}

	public String searchByBusinessId(String businessID) {
		OAuthRequest request = createOAuthRequest(BUSINESS_PATH + "/" + businessID);
		return sendRequestAndGetResponse(request);
	}

	private OAuthRequest createOAuthRequest(String path) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://" + API_HOST + path);
		return request;
	}

	private String sendRequestAndGetResponse(OAuthRequest request) {
		System.out.println("Querying " + request.getCompleteUrl() + " ...");
		this.service.signRequest(this.accessToken, request);
		Response response = request.send();
		return response.getBody();
	}

	public JSONObject queryAPI(YelpAPI yelpApi, String term, String location) {
		String searchResponseJSON = yelpApi.searchForBusinessesByLocation(term, location);

		JSONParser parser = new JSONParser();
		JSONObject response = null;
		try {
			response = (JSONObject) parser.parse(searchResponseJSON);
		} catch (ParseException pe) {
			System.out.println("Error: could not parse JSON response:");
			System.out.println(searchResponseJSON);
			System.exit(1);
		}

		JSONArray businesses = (JSONArray) response.get("businesses");
		JSONObject firstBusiness = (JSONObject) businesses.get(0);
		String firstBusinessID = firstBusiness.get("id").toString();
		System.out.println(String.format("%s businesses found, querying business info for the top result \"%s\" ...",
				businesses.size(), firstBusinessID));

		// Select the first business and display business details
		String businessResponseJSON = yelpApi.searchByBusinessId(firstBusinessID.toString());
		System.out.println(String.format("Result for business \"%s\" found:", firstBusinessID));
		System.out.println(businessResponseJSON);
		return firstBusiness;
	}

	private static JSONArray queryAPI(YelpAPI yelpApi, String term, String location, int limit) 
	{
		String searchResponseJSON = yelpApi.searchForBusinessesByLocation(term, location, limit);

		JSONParser parser = new JSONParser();
		JSONObject response = null;
		try {
			response = (JSONObject) parser.parse(searchResponseJSON);
		} catch (ParseException pe) {
			System.out.println("Error: could not parse JSON response:");
			System.out.println(searchResponseJSON);
			System.exit(1);
		}

		JSONArray businesses = (JSONArray) response.get("businesses");
		return businesses;
	}

	private static JSONArray queryAPI(YelpAPI yelpApi, String term, String location, int limit, int radius) 
	{
		String searchResponseJSON = yelpApi.searchForBusinessesByLocation(term, location, limit, radius);

		JSONParser parser = new JSONParser();
		JSONObject response = null;
		try {
			response = (JSONObject) parser.parse(searchResponseJSON);
		} catch (ParseException pe) {
			System.out.println("Error: could not parse JSON response:");
			System.out.println(searchResponseJSON);
			System.exit(1);
		}

		JSONArray businesses = (JSONArray) response.get("businesses");
		return businesses;
	}

	public Business[] returnBusinesses(YelpAPI yelp, String term, String location, int limit) {
		Business[] businesses = new Business[limit];
		JSONArray businessJSON = queryAPI(yelp, term, location, limit);

		return fillArray(businesses, businessJSON);
	}

	public Business[] returnBusinesses(YelpAPI yelp, String term, String location, String param, int limit, int radius) {
		Business[] businesses = new Business[limit];
		JSONArray businessJSON = queryAPI(yelp, term, location, limit, radius);

		return fillArray(businesses, businessJSON);
	}
	
	public String[] returnParam(YelpAPI yelp, String term, String location, int limit, String param)
	{
		JSONArray businesses = queryAPI(yelp, term, location, limit);
		String[] listOfParam = new String[limit];

		for (int i = 0; i < businesses.size(); i++) 
		{
			JSONObject obj = (JSONObject) businesses.get(i);
			listOfParam[i] = (String) obj.get(param);
		}
		return listOfParam;
	}	
	
	public String[] returnParam(YelpAPI yelp, String term, String location, int limit, int radius, String param)
	{
		JSONArray businesses = queryAPI(yelp, term, location, limit, radius);
		String[] listOfParam = new String[limit];

		for (int i = 0; i < businesses.size(); i++) 
		{
			JSONObject obj = (JSONObject) businesses.get(i);
			listOfParam[i] = (String) obj.get(param);
		}
		return listOfParam;
	}
	public Business[] fillArray(Business[] businesses, JSONArray businessJSON)
	{
		for (int i = 0; i < businessJSON.size(); i++) {
			Business b = new Business();

			JSONObject obj = (JSONObject) businessJSON.get(i);
			JSONObject loc = (JSONObject) obj.get("location");
			JSONObject coord = (JSONObject) loc.get("coordinate");

			b.setName((String) obj.get("name"));
			try
			{
				b.setLocationAddress((String) ((JSONArray) (loc.get("address"))).get(0));
			}
			catch (Exception e)
			{
				// e.printStackTrace();
				// Rare case of no address. Will be set to null.
				System.out.println("Null address. Skipping.");
			}
			b.setCity((String) loc.get("city"));
			b.setState((String) loc.get("state_code"));
			b.setZipCode((String) loc.get("postal_code"));
			b.setCountryCode((String) loc.get("country_code"));
			b.setUrl((String) obj.get("url"));
			b.setPhone((String) obj.get("phone"));
			b.setRating((double) obj.get("rating"));
			b.setLatitude((double) coord.get("latitude"));
			b.setLongitude((double) coord.get("longitude"));

			businesses[i] = b;
		}
		return businesses;
	}
	public Business[] fillArray2(Business[] businesses, JSONArray businessJSON)
	{
		for (int i = 0; i < businessJSON.size(); i++) {
			BusinessBuilder b = new BusinessBuilder();
					
			JSONObject obj = (JSONObject) businessJSON.get(i);
			JSONObject loc = (JSONObject) obj.get("location");
			JSONObject coord = (JSONObject) loc.get("coordinate");

			b.withName((String) obj.get("name"));
			try
			{
				b.withLocationAddress((String) ((JSONArray) (loc.get("address"))).get(0));
			}
			catch (Exception e)
			{
				// e.printStackTrace();
				// Rare case of no address. Will be set to null.
				System.out.println("Null address. Skipping.");
			}
			b.withCity((String) loc.get("city"));
			b.withState((String) loc.get("state_code"));
			b.withZipCode((String) loc.get("postal_code"));
			b.withCountryCode((String) loc.get("country_code"));
			b.withUrl((String) obj.get("url"));
			b.withPhone((String) obj.get("phone"));
			b.withRating((double) obj.get("rating"));
			b.withLatitude((double) coord.get("latitude"));
			b.withLongitude((double) coord.get("longitude"));

			businesses[i] = b.build();
		}
		return businesses;
	}
}
