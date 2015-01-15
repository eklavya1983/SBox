package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.Entry;
import play.libs.Json;
import play.mvc.*;
import play.db.jpa.JPA;

import views.html.*;

import javax.persistence.Query;
import java.util.List;

public class Application extends Controller {
    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }

    @play.db.jpa.Transactional
    public static Result insertEntry() {
        Http.RequestBody body = request().body();
        JsonNode jsonNode = body.asJson();
        Entry entry = Json.fromJson(jsonNode, Entry.class);
        JPA.em().persist(entry);
        return Results.ok();
    }

    @play.db.jpa.Transactional(readOnly=true)
    public static Result getResults() {
        Query q = JPA.em().createQuery("Select e from Entry e");
        List<Entry> list = q.getResultList();
        return Results.ok(Json.toJson(list));
    }

    public static Result search() {
        return Results.TODO;
    }
}
