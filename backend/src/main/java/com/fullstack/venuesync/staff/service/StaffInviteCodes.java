package com.fullstack.venuesync.staff.service;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;

/**
 * Invite codes: 10 characters of Crockford base32 (about 50 bits), shown as {@code K7Q2M-9XH4P}. The alphabet has
 * no I, L, O or U, and reading tolerates the usual mix-ups (O for 0, I or L for 1), lower case, spaces and dashes,
 * so a code read out over the phone still works.
 */
public final class StaffInviteCodes {

  static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
  static final int LENGTH = 10;
  private static final SecureRandom RANDOM = new SecureRandom();

  private StaffInviteCodes() {
  }

  public static String generate() {
    StringBuilder code = new StringBuilder(LENGTH);
    for (int i = 0; i < LENGTH; i++) {
      code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
    }
    return code.toString();
  }

  /** The stored form of what someone typed, or empty when it can't be one of our codes. */
  public static Optional<String> normalize(String input) {
    if (input == null) {
      return Optional.empty();
    }
    String code = input.toUpperCase(Locale.ROOT)
        .replaceAll("[\\s-]", "")
        .replace('O', '0')
        .replace('I', '1')
        .replace('L', '1');
    if (code.length() != LENGTH || !code.chars().allMatch(c -> ALPHABET.indexOf(c) >= 0)) {
      return Optional.empty();
    }
    return Optional.of(code);
  }

  public static String format(String code) {
    return code.substring(0, 5) + "-" + code.substring(5);
  }
}
