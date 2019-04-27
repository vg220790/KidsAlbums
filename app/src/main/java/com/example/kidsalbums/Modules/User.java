package com.example.kidsalbums.Modules;

import com.example.kidsalbums.Modules.Child;
import com.example.kidsalbums.Modules.Kindergarten;

public class User {

    private String id;
    private String email;
    private String name;
    private String phone;
    private Child child;
    private Kindergarten kindergarten;


    public User(){
        this.email = "";
        this.name = "";
        this.phone = "";
    }

    public User(String id, String name, String phone, String email){
        setId(id);
        setName(name);
        setEmail(email);
        setPhoneNumber(phone);
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phone;
    }

    public void setPhoneNumber(String phone) {
        this.phone = phone;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getId() {
        return this.id;
    }

    public Kindergarten getKindergarten() {
        return this.getChild().getKindergarten();
    }

    public Child getChild() {
        return this.child;
    }

    public void setChild(Child child) {
        this.child = child;
    }

}
