package com.crearo.okayphone.managers.xml.classes;

/**
 * Created by francescoandreuzzi on 06/03/2018.
 */

public interface XMLPrefsElement {
    XMLPrefsList getValues();

    void write(XMLPrefsSave save, String value);

    String[] deleted();
}