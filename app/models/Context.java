package models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.List;

/**
 * Created on 1/15/15.
 */
// TODO(eklavya1983): Make combination of tag name + uid as the key
@Entity
@NamedQuery(name="Context.search", query = "SELECT c FROM Context c WHERE c.name = :name AND c.userid = :userid")
public class Context {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private String name;
    private String userid;
    @ManyToMany(mappedBy = "contexts")
    @JsonIgnore
    private List<Entry> entries;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }
}
