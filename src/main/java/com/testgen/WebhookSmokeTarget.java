package com.testgen;

public class WebhookSmokeTarget {

    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public String farewell(String name) {
        return "Goodbye, " + name + "!";
    }

    public String shout(String name) {
        return name.toUpperCase() + "!!!";
    }

    public String whisper(String name) {
        return name.toLowerCase() + "...";
    }
}
