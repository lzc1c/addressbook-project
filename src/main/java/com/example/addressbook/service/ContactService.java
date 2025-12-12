package com.example.addressbook.service;

import com.example.addressbook.model.Contact;
import com.example.addressbook.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactService {

    @Autowired
    private ContactRepository contactRepository;

    public List<Contact> getAllContacts() {
        return contactRepository.findAll();
    }

    public List<Contact> getBookmarkedContacts() {
        return contactRepository.findBookmarkedContacts();
    }

    public Contact createContact(Contact contact) {
        return contactRepository.save(contact);
    }

    // ✅ 新增：更新联系人
    public Contact updateContact(Contact contact) {
        if (!contactRepository.existsById(contact.getId())) {
            return null; // 或抛出异常
        }
        return contactRepository.save(contact);
    }

    public Contact updateBookmark(Long id, boolean bookmarked) {
        Contact contact = contactRepository.findById(id).orElse(null);
        if (contact != null) {
            contact.setBookmarked(bookmarked);
            return contactRepository.save(contact);
        }
        return null;
    }

    public void deleteContact(Long id) {
        contactRepository.deleteById(id);
    }
}