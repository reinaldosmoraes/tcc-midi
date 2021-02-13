package com.example.drumtechtutorial;

public class Helper {
    public static byte teste(byte message) {
        return (byte) ((message & 0xF0) >> 4);
    }
}
