package com.jnet.system.controller;

import lombok.Data;

public class ResetPasswordRequest {

    private String newPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

}
