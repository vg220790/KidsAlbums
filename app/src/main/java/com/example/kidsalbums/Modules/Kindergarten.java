package com.example.kidsalbums.Modules;

import java.util.ArrayList;

public class Kindergarten {
    private String id;
    private String name;
    private String address;
    private String email;
    private double latitude;
    private double longitude;

    public Kindergarten(){
        this.id = "";
        this.name = "";
        this.address = "";
        this.email = "";
    }

    public Kindergarten(String id, String name, String address, String email){
        this.setId(id);
        this.setName(name);
        this.setAddress(address);
        this.setEmail(email);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getEmail() {
        return email;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

}
