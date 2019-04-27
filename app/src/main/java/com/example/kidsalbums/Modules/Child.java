package com.example.kidsalbums.Modules;

public class Child {

    private String id;
    private String name;
    private String birthday;
    private String tag;
    private Kindergarten kindergarten;

    public Child(){
        this.id = "";
        this.name = "";
        this.birthday = "";
        this.tag = "";
    }

    public Child(String id, String name, String birthday, String tag){
        setId(id);
        setName(name);
        setBirthday(birthday);
        setTag(tag);
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String full_name) {
        this.name = full_name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setKindergarten(Kindergarten kindergarten) { this.kindergarten = kindergarten; }

    public Kindergarten getKindergarten() { return this.kindergarten; }
}
