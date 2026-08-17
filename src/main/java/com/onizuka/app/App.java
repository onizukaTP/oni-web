package com.onizuka.app;

import com.onizuka.framework.OniWeb;

public class App {
    public static void main(String[] args) {
        OniWeb.start(App.class, 8080);
    }
}
