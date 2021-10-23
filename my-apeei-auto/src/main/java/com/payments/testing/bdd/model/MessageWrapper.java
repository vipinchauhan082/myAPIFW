package com.payments.testing.bdd.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MessageWrapper {

    private String messageId;
    private String message;
    private String subscriberId;
    private byte[] messageArray;

}
