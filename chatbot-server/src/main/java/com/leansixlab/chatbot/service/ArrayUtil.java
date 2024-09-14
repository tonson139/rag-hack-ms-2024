package com.leansixlab.chatbot.service;

public class ArrayUtil {
    public static float[] concatenateArrays(float[] arr1, float[] arr2) {
        int totalLength = arr1.length + arr2.length;
        float[] result = new float[totalLength];

        System.arraycopy(arr1, 0, result, 0, arr1.length);
        System.arraycopy(arr2, 0, result, arr1.length, arr2.length);

        return result;
    }
}
