package com.c0124.k9.controller;

import com.c0124.k9.mail.Message;

public interface MessageRemovalListener {
    public void messageRemoved(Message message);
}
