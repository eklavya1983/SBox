package models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * Created on 1/14/15.
 */
@Entity
public class Entry {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long eid;
    private String userid;
    private long timestamp;
    private String location;
    private String mimeType;
    private String uri;
    private String text;
    @ElementCollection
    private List<String> contexts;

    public List<String> getContexts() {
        return contexts;
    }

    public void setContexts(List<String> contexts) {
        this.contexts = contexts;
    }

    public long getEid() {
        return eid;
    }

    public void setEid(long eid) {
        this.eid = eid;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "eid: " + eid + " text: " + text;
    }

}
