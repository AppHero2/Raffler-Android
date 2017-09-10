package com.raffler.app.interfaces;

import com.raffler.app.models.Contact;
import com.raffler.app.models.User;

import java.util.List;

/**
 * Created by Ghost on 9/9/2017.
 */

public interface ContactFinderListener {
    void onLoadedContact(boolean success, Contact contact);
}
