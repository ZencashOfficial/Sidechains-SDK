package com.horizen.cryptolibprovider;

import com.horizen.librustsidechains.FieldElement;

import java.util.Arrays;

public class FieldElementUtils {
    public static int maximumFieldElementLength() {
        return FieldElement.FIELD_ELEMENT_LENGTH;
    }

    public static FieldElement messageToFieldElement(byte[] message) {
        if (message.length > maximumFieldElementLength()) {
            throw new IllegalArgumentException("Message length is exceed allowed message len. Message len " +
                    message.length + " but it shall be less than " + maximumFieldElementLength());
        }
        return FieldElement.deserialize(Arrays.copyOf(message, maximumFieldElementLength()));
    }



}
