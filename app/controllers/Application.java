package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.EntryContext;
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

        /* First persist the entry */
        JPA.em().persist(entry);
        Logger.info("Added new entry: " + entry.getEid());

        /* Update the tags */
        entry.getContexts().stream().forEach(cName -> {
            EntryContext ec;
            TypedQuery<EntryContext> query = JPA.em().createNamedQuery("EntryContext.find", EntryContext.class);
            query.setParameter("name", cName);
            query.setParameter("userid", entry.getUserid());
            List<EntryContext> resultList = query.getResultList();
            if (resultList == null || resultList.size() == 0) {
                Logger.info("Adding new context: " + cName);
                ec = new EntryContext();
                ec.setName(cName);
                ec.setUserid(entry.getUserid());
                ec.setEntries(new ArrayList<Entry>());
            } else {
                assert(resultList.size() == 1);
                ec = resultList.get(0);
            }
            ec.getEntries().add(entry);
            JPA.em().persist(ec);
            Logger.info("Added/updated context: " + ec.getId());
        });

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

    public static Result getEntries() {
        String cName = request().queryString("cName");
        // TODO: Get from session
        String userid = "Rao";
        List<Entry> entries = null;
        TypedQuery<EntryContext> query = JPA.em().createNamedQuery("EntryContext.find", EntryContext.class);
        query.setParameter("name", cName);
        query.setParameter("userid", userid);
        List<EntryContext> resultList = query.getResultList();
        if (resultList != null && resultList.size() > 0) {
            entries = resultList.get(0).getEntries();
        }
        return Results.ok(Json.toJson(entries));
    }
}
