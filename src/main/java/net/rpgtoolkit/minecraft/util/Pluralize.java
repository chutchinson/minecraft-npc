/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rpgtoolkit.minecraft.util;

/**
 * Utility class for obtaining a plural version of a 
 * singular English noun or pronoun. Not accurate for all
 * known specialized pluralizations and may not encompass
 * all defined pluralization rules.
 */
public class Pluralize {
    
    private static final String CONSONANTS = "bcdfghjklmnpqrstvwxz";
    
    /**
     * Converts the supplied text into a pluralized version if
     * and only if the text represents a single noun or pronoun
     * and quantity is greater than one.
     * 
     * @param text text (noun or pronoun) to pluralize
     * @param quantity quantity represented by text
     * @return pluralized text or original text if not applicable
     */
    public static String apply(String text, int quantity) {
        
        if (quantity <= 1) {
            return text;
        }
        
        String noun = text.toLowerCase();
        String suffix = noun.substring(noun.length() - 2, noun.length() - 1);
        
        if (noun.endsWith("o") && CONSONANTS.contains(suffix)) {
            return text + "es";
        }
        if (noun.endsWith("y") && CONSONANTS.contains(suffix)) {
            return text.substring(0, noun.length() - 1) + "ies";
        }
        if (noun.endsWith("s") || noun.endsWith("sh") || noun.endsWith("ch") ||
                noun.endsWith("x") || noun.endsWith("z")) {
            return text + "es";
        }
        
        return text + "s";
        
    }
    
}
