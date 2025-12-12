// src/main/java/com/example/addressbook/model/ContactMethod.java
package com.example.addressbook.model;

import jakarta.persistence.*;

@Entity
@Table(name = "contact_method")
public class ContactMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String methodType; // "phone", "email", etc.
    private String value;

    public ContactMethod() {}
    public ContactMethod(String methodType, String value) {
        this.methodType = methodType;
        this.value = value;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMethodType() { return methodType; }
    public void setMethodType(String methodType) { this.methodType = methodType; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}