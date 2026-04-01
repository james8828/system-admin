package com.jnet.common.security.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AuthorityUtils {

    public static List<String> getAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return new ArrayList<>();
        }
        Collection<? extends org.springframework.security.core.GrantedAuthority> authorities = authentication.getAuthorities();
        List<String> authorityList = new ArrayList<>();
        for (org.springframework.security.core.GrantedAuthority authority : authorities) {
            authorityList.add(authority.getAuthority());
        }
        return authorityList;
    }

    public static List<String> getPermissions() {
        return getAuthorities();
    }

    public static boolean hasPermission(String permission) {
        List<String> authorities = getAuthorities();
        return authorities.contains(permission);
    }


    public static boolean hasRole(String role) {
        List<String> authorities = getAuthorities();
        return authorities.contains("ROLE_" + role) || authorities.contains(role);
    }

}
