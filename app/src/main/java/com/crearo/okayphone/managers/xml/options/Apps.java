package com.crearo.okayphone.managers.xml.options;

import com.crearo.okayphone.managers.AppsManager;
import com.crearo.okayphone.managers.xml.classes.XMLPrefsElement;
import com.crearo.okayphone.managers.xml.classes.XMLPrefsSave;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Apps implements XMLPrefsSave {

    default_app_n1 {
        @Override
        public String defaultValue() {
            return MOST_USED;
        }

        @Override
        public String info() {
            return "The first default-suggested app";
        }
    },
    default_app_n2 {
        @Override
        public String defaultValue() {
            return MOST_USED;
        }

        @Override
        public String info() {
            return "The second default-suggested app";
        }
    },
    default_app_n3 {
        @Override
        public String defaultValue() {
            return "com.android.vending";
        }

        @Override
        public String info() {
            return "The third default-suggested app";
        }
    },
    default_app_n4 {
        @Override
        public String defaultValue() {
            return NULL;
        }

        @Override
        public String info() {
            return "The fourth default-suggested app";
        }
    },
    default_app_n5 {
        @Override
        public String defaultValue() {
            return NULL;
        }

        @Override
        public String info() {
            return "The fifth default-suggested app";
        }
    },
    app_groups_sorting {
        @Override
        public String defaultValue() {
            return "2";
        }

        @Override
        public String info() {
            return "0 = time up->down; 1 = time down->up; 2 = alphabetical up->down; 3 = alphabetical down->up; 4 = most used up->down; 5 = most used down->up";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }
    };

    public static final String MOST_USED = "most_used";
    public static final String NULL = "null";

    @Override
    public String label() {
        return name();
    }

    @Override
    public XMLPrefsElement parent() {
        return AppsManager.instance;
    }

    @Override
    public boolean is(String s) {
        return name().equals(s);
    }

    @Override
    public String type() {
        return XMLPrefsSave.APP;
    }
}
