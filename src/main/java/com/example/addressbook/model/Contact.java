// src/main/java/com/example/addressbook/model/Contact.java
package com.example.addressbook.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "contact")
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private boolean bookmarked;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "contact_id")
    private List<ContactMethod> methods;

    // Constructors
    public Contact() {}
    public Contact(String name) { this.name = name; }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean getBookmarked() { return bookmarked; }
    public void setBookmarked(boolean bookmarked) { this.bookmarked = bookmarked; }
    public List<ContactMethod> getMethods() { return methods; }
    public void setMethods(List<ContactMethod> methods) { this.methods = methods; }
}