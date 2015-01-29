package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.Context;
import models.Entry;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.*;
import play.db.jpa.JPA;

import views.html.*;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;

class QueryParamsBuilder {
    private String path = "";
    private boolean first = true;
    public QueryParamsBuilder setQueryParameter(String name, String value) {
        try {
            if (!first) {
                path += "&";
            }
            first = false;
            path += URLEncoder.encode(name, "UTF-8");
            path += "=";
            path += URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Broken VM does not support UTF-8");
        }
        return this;
    }

    public String toString() {
        return path;
    }
}

class SimpleUriBuilder {
    private String path;
    boolean firstParam = true;

    public SimpleUriBuilder(String path) {
        this.path = path;
    }

    public SimpleUriBuilder addParameter(String name, String value) {
        if (firstParam) {
            this.path += "?";
            firstParam = false;
        } else {
            this.path += "&";
        }
        encode(name, value);
       return this;
    }

    public String getPath() {
        return path;
    }

    public String toString() {
        return getPath();
    }

    private void encode(String name, String value) {
        try {
            path += URLEncoder.encode(name, "UTF-8");
            path += "=";
            path += URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Broken VM does not support UTF-8");
        }
    }

}

class GoogleOAuthHelper {
    private String clientId;
    private String clientSecret;
    private String redirecUri;
    private final static String AUTH_EP = "https://accounts.google.com/o/oauth2/auth";
    private final static String TOKEN_EP = "https://www.googleapis.com/oauth2/v3/token";
    private final static String USER_PROFILE_EP = "https://www.googleapis.com/plus/v1/people/me";
    private final static int REQUEST_TIMEOUT = 10000;

    GoogleOAuthHelper(String clientId, String clientSecret, String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirecUri = redirectUri;
    }

    Result getLoginResult() {
        SimpleUriBuilder builder = new SimpleUriBuilder(AUTH_EP);
        builder.addParameter("response_type", "code")
                .addParameter("client_id", clientId)
                .addParameter("redirect_uri", redirecUri)
                .addParameter("scope", "profile")
                // todo: Generate a unique state
                .addParameter("state", "SBoxState");
        String authUrl = builder.toString();
        Logger.info("authentication url is: " + authUrl);

        return Results.redirect(authUrl);
    }

    String handleOAuthCallback(Http.Request request) {
        // TODO: verify the state value
        /* Parse out the code */
        String code = request.getQueryString("code");
        /* Send a request to get access token */
        QueryParamsBuilder builder = new QueryParamsBuilder();
        builder.setQueryParameter("code", code)
                .setQueryParameter("client_id", clientId)
                .setQueryParameter("client_secret", clientSecret)
                .setQueryParameter("redirect_uri", redirecUri)
                .setQueryParameter("grant_type", "authorization_code");
        String postData = builder.toString();
        WSResponse wsResponse = WS.url(TOKEN_EP)
                .setContentType("application/x-www-form-urlencoded")
                .post(postData).get(REQUEST_TIMEOUT);
        JsonNode jsonNode = wsResponse.asJson();
        String accessToken = jsonNode.get("access_token").asText();

        /* We have access token.  Issue get to get the profile info */
        wsResponse = WS.url(USER_PROFILE_EP)
                .setHeader("Authorization", "Bearer " + accessToken)
                .get().get(REQUEST_TIMEOUT);
        Logger.info("person profile response is: " + new String(wsResponse.asByteArray()));
        jsonNode = wsResponse.asJson();
        String personId = jsonNode.get("id").asText();
        Logger.info("Id is: " + personId);
        // todo: Parse into a person profile class and return that
        return personId;

    }
}

public class Application extends Controller {
    // todo: Store this in cache for each user
    static GoogleOAuthHelper authHelper;

    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }

    @play.db.jpa.Transactional
    public static Result insertEntry() {
        /* Parse out entry */
        Http.RequestBody body = request().body();
        JsonNode jsonNode = body.asJson();
        Entry entry = Json.fromJson(jsonNode, Entry.class);

        List<Context> contexts = entry.getContexts()
                .stream()
                .map(ic -> {
                    /* See if the context exists already */
                    TypedQuery<Context> query = JPA.em().createNamedQuery("Context.search", Context.class);
                    query.setParameter("name", ic.getName());
                    // TODO(ekl): Get userid from session
                    query.setParameter("userid", entry.getUserid());
                    List<Context> resultList = query.getResultList();
                    if (resultList == null || resultList.size() == 0) {
                        Logger.info("New context: " + ic.getName());
                        // TODO: Possible side effect
                        ic.setUserid(entry.getUserid());
                        return ic;
                    } else {
                        // TODO: assert
                        return resultList.get(0);
                    }
                })
                .collect(Collectors.toList());
        entry.setContexts(contexts);
        JPA.em().persist(entry);

        return Results.ok(Json.toJson(entry.getEid()));
    }

    public static void updateEntry(long eid) {

    }

    public static void addTags(long eid) {

    }

    public static void removeTags(long eid) {

    }


    @play.db.jpa.Transactional(readOnly = true)
    public static Result getAllResults() {
        Query q = JPA.em().createQuery("Select e from Entry e");
        List<Entry> list = q.getResultList();
        return Results.ok(Json.toJson(list));
    }

    @play.db.jpa.Transactional(readOnly = true)
    public static Result getResults() {
        Query q = JPA.em().createQuery("Select e from Entry e");
        List<Entry> list = q.getResultList();
        return Results.ok(Json.toJson(list));
    }

    @play.db.jpa.Transactional(readOnly = true)
    public static Result getEntries() {
        String cName = request().getQueryString("cName");
        // TODO: Get from session
        String userid = "Rao";
        TypedQuery<Entry> query = JPA.em().createNamedQuery("Entry.search", Entry.class);
        query.setParameter("cName", cName);
        List<Entry> resultList = query.getResultList();
        return Results.ok(Json.toJson(resultList));
    }

    @play.db.jpa.Transactional(readOnly = true)
    public static Result getEntry() {
        long eid = Long.parseLong(request().getQueryString("eid"));
        Entry e = JPA.em().find(Entry.class, eid);
        return Results.ok(Json.toJson(e));
    }


    public static Result login() {
       authHelper = new GoogleOAuthHelper("719994656729-tvik9l3jlasi3hc0u5ll3t94d3rj9nbr.apps.googleusercontent.com",
                "MCRPRzei3bOiCPPzNjmKppF0",
                "http://localhost:9000/oauth2callback");
        return authHelper.getLoginResult();

//        SimpleUriBuilder builder = new SimpleUriBuilder("https://accounts.google.com/o/oauth2/auth");
//        builder.addParameter("response_type", "code")
//                .addParameter("client_id", "719994656729-tvik9l3jlasi3hc0u5ll3t94d3rj9nbr.apps.googleusercontent.com")
//                .addParameter("redirect_uri", "http://localhost:9000/oauth2callback")
//                .addParameter("scope", "profile")
//                .addParameter("state", "SBoxState");
//        String authUrl = builder.toString();
//        Logger.info("url is: " + authUrl);
//
//        return Results.redirect(authUrl);
    }

    public static Result oauth2callback()
    {
        String id = authHelper.handleOAuthCallback(request());
        return ok(id);

//        // TODO: verify the state value
//        /* Parse out the code */
//        String code = request().getQueryString("code");
//        /* Send a request to get access token */
//        QueryParamsBuilder builder = new QueryParamsBuilder();
//        builder.setQueryParameter("code", code)
//                .setQueryParameter("client_id", "719994656729-tvik9l3jlasi3hc0u5ll3t94d3rj9nbr.apps.googleusercontent.com")
//                .setQueryParameter("client_secret", "MCRPRzei3bOiCPPzNjmKppF0")
//                .setQueryParameter("redirect_uri", "http://localhost:9000/oauth2callback")
//                .setQueryParameter("grant_type", "authorization_code");
//                //.setQueryParameter("redirect_uri", "http://localhost:9000/tokencallback")
//        String postData = builder.toString();
//        WSResponse wsResponse = WS.url("https://www.googleapis.com/oauth2/v3/token")
//                .setContentType("application/x-www-form-urlencoded")
//                .post(postData).get(REQUEST_TIMEOUT);
//        JsonNode jsonNode = wsResponse.asJson();
//        String accessToken = jsonNode.get("access_token").asText();
//
//        /* We have access token.  Issue get to get the profile info */
//        wsResponse = WS.url("https://www.googleapis.com/plus/v1/people/me")
//                .setHeader("Authorization", "Bearer " + accessToken)
//                .get().get(REQUEST_TIMEOUT);
//        jsonNode = wsResponse.asJson();
//        String personId = jsonNode.get("id").asText();
//        Logger.info("Id is: " + personId);
//
//
//        return ok("Hello: " + personId);
    }
}
