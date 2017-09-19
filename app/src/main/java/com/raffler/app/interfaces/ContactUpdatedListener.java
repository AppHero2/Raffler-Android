package com.raffler.app.interfaces;

import com.raffler.app.models.Contact;

import java.util.List;

/**
 * Created by Ghost on 9/18/2017.
 */

public interface ContactUpdatedListener {
    void onUpdatedContacts(List<Contact> contacts);
}
