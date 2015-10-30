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

/**
 * Code sample for accessing the Yelp API V2.
 * 
 * This program demonstrates the capability of the Yelp API version 2.0 by using
 * the Search API to query for businesses by a search term and location, and the
 * Business API to query additional information about the top result from the
 * search query.
 * 
 * <p>
 * See <a href="http://www.yelp.com/developers/documentation">Yelp
 * Documentation</a> for more info.
 * 
 */
public class YelpAPI {

	private static final String API_HOST = "api.yelp.com";
	// private static final String DEFAULT_TERM = "Happy Hour";
	private static final int SEARCH_LIMIT = 3;
	private static final String SEARCH_PATH = "/v2/search";
	private static final String BUSINESS_PATH = "/v2/business";

	/*
	 * Update OAuth credentials below from the Yelp Developers API site:
	 * http://www.yelp.com/developers/getting_started/api_access
	 */
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
			throw new RuntimeException("Init failed.");
		}
	}

	/**
	 * Setup the Yelp API OAuth credentials.
	 * 
	 * @param consumerKey
	 *            Consumer key
	 * @param consumerSecret
	 *            Consumer secret
	 * @param token
	 *            Token
	 * @param tokenSecret
	 *            Token secret
	 */
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

	/**
	 * Creates and sends a request to the Search API by term and location.
	 * <p>
	 * See
	 * <a href="http://www.yelp.com/developers/documentation/v2/search_api">Yelp
	 * Search API V2</a> for more info.
	 * 
	 * @param term
	 *            <tt>String</tt> of the search term to be queried
	 * @param location
	 *            <tt>String</tt> of the location
	 * @return <tt>String</tt> JSON Response
	 */
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

	/**
	 * Creates and sends a request to the Business API by business ID.
	 * <p>
	 * See
	 * <a href="http://www.yelp.com/developers/documentation/v2/business">Yelp
	 * Business API V2</a> for more info.
	 * 
	 * @param businessID
	 *            <tt>String</tt> business ID of the requested business
	 * @return <tt>String</tt> JSON Response
	 */
	public String searchByBusinessId(String businessID) {
		OAuthRequest request = createOAuthRequest(BUSINESS_PATH + "/" + businessID);
		return sendRequestAndGetResponse(request);
	}

	/**
	 * Creates and returns an {@link OAuthRequest} based on the API endpoint
	 * specified.
	 * 
	 * @param path
	 *            API endpoint to be queried
	 * @return <tt>OAuthRequest</tt>
	 */
	private OAuthRequest createOAuthRequest(String path) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://" + API_HOST + path);
		return request;
	}

	/**
	 * Sends an {@link OAuthRequest} and returns the {@link Response} body.
	 * 
	 * @param request
	 *            {@link OAuthRequest} corresponding to the API request
	 * @return <tt>String</tt> body of API response
	 */
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

	private static JSONArray queryAPI(YelpAPI yelpApi, String term, String location, int limit) {
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

	private static JSONArray queryAPI(YelpAPI yelpApi, String term, String location, int limit, int radius) {
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

	public Business[] returnBusinesses(YelpAPI yelp, String term, String location, String param, int limit,
			int radius) {
		Business[] businesses = new Business[limit];

		JSONArray businessJSON = queryAPI(yelp, term, location, limit, radius);
		// String[] listOfParam = new String[limit];

		for (int i = 0; i < businessJSON.size(); i++) {
			Business b = new Business();

			JSONObject obj = (JSONObject) businessJSON.get(i);
			JSONObject loc = (JSONObject) obj.get("location");
			JSONObject coord = (JSONObject) loc.get("coordinate");

			b.setName((String) obj.get("name"));
			try {
				b.setLocationAddress((String) ((JSONArray) (loc.get("address"))).get(0));
			} catch (Exception e) {
				// e.printStackTrace();
				// No address?
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
			// listOfParam[i] = (String) obj.get(param);
		}
		return businesses;
	}

	public Business[] returnBusinesses(YelpAPI yelp, String term, String location, String param, int limit) {
		Business[] businesses = new Business[limit];
		JSONArray businessJSON = queryAPI(yelp, term, location, limit);

		for (int i = 0; i < businessJSON.size(); i++) {
			Business b = new Business();

			JSONObject obj = (JSONObject) businessJSON.get(i);
			JSONObject loc = (JSONObject) obj.get("location");
			JSONObject coord = (JSONObject) loc.get("coordinate");

			b.setName((String) obj.get("name"));
			try {
				b.setLocationAddress((String) ((JSONArray) (loc.get("address"))).get(0));
			} catch (Exception e) {
				// e.printStackTrace();
				// No address?
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

	public String[] returnParam(YelpAPI yelp, String term, String location, String param, int limit, int radius) {
		JSONArray businesses = queryAPI(yelp, term, location, limit, radius);
		String[] listOfParam = new String[limit];

		for (int i = 0; i < businesses.size(); i++) {
			JSONObject obj = (JSONObject) businesses.get(i);
			listOfParam[i] = (String) obj.get(param);
		}
		System.out.println(businesses.get(0));
		return listOfParam;
	}
}
