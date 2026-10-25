package com.neaplugins.welcomer.constants;

/**
 * Central repository of permission nodes used throughout NeaWelcomer.
 */
public final class Permissions {

    /**
     * Grants access to administrative commands (/neawelcomer reload, info, help).
     */
    public static final String ADMIN = "neawelcomer.admin";

    /**
     * Determines whether a player can see standard join broadcast messages and hear sounds.
     */
    public static final String JOIN_SEE = "neawelcomer.join.see";

    /**
     * Determines whether a player can see quit broadcast messages and hear sounds.
     */
    public static final String QUIT_SEE = "neawelcomer.quit.see";

    /**
     * Determines whether a player can see first-join broadcast messages and hear sounds.
     */
    public static final String FIRSTJOIN_SEE = "neawelcomer.firstjoin.see";

    private Permissions() {
        // Constant utility class - prevent instantiation
    }
}
