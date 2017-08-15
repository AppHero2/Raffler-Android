package com.raffler.app.country;

/**
 * Created by Ghost on 14/8/2017.
 */

public interface CountryPickerListener {
    public void onSelectCountry(String name, String code, String dialCode, int flagDrawableResID);
}
