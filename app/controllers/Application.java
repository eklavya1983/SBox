package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.Context;
import models.Entry;
import play.Logger;
import play.libs.Json;
import play.mvc.*;
import play.db.jpa.JPA;

import views.html.*;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Application extends Controller {
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


    @play.db.jpa.Transactional(readOnly=true)
    public static Result getAllResults() {
        Query q = JPA.em().createQuery("Select e from Entry e");
        List<Entry> list = q.getResultList();
        return Results.ok(Json.toJson(list));
    }
    @play.db.jpa.Transactional(readOnly=true)
    public static Result getResults() {
        Query q = JPA.em().createQuery("Select e from Entry e");
        List<Entry> list = q.getResultList();
        return Results.ok(Json.toJson(list));
    }

    @play.db.jpa.Transactional(readOnly=true)
    public static Result getEntries() {
        String cName = request().getQueryString("cName");
        // TODO: Get from session
        String userid = "Rao";
        TypedQuery<Entry> query = JPA.em().createNamedQuery("Entry.search", Entry.class);
        query.setParameter("cName", cName);
        List<Entry> resultList = query.getResultList();
        return Results.ok(Json.toJson(resultList));
    }

    @play.db.jpa.Transactional(readOnly=true)
    public static Result getEntry() {
        long eid = Long.parseLong(request().getQueryString("eid"));
        Entry e  = JPA.em().find(Entry.class, eid);
        return Results.ok(Json.toJson(e));
    }
}
