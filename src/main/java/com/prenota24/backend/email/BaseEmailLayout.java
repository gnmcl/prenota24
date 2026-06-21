package com.prenota24.backend.email;

import java.time.Year;

/**
 * Generates branded HTML email wrappers for all Prenota24 transactional emails.
 *
 * <p>Uses table-based layout with fully inline styles for maximum compatibility
 * across email clients (Gmail, Outlook, Apple Mail, mobile).
 *
 * <p>Brand tokens mirror the frontend design system:
 * <ul>
 *   <li>Primary:      #4F46E5 (indigo)</li>
 *   <li>Primary dark: #3730A3</li>
 *   <li>Background:   #F9FAFB</li>
 *   <li>Card:         #FFFFFF</li>
 *   <li>Accent bg:    #EEF2FF (indigo-50)</li>
 * </ul>
 */
public final class BaseEmailLayout {

    // ── Brand tokens ──────────────────────────────────────────────────────────

    private static final String PRIMARY       = "#4F46E5";
    private static final String PRIMARY_DARK  = "#3730A3";
    private static final String TEXT          = "#111827";
    private static final String TEXT_MUTED    = "#6B7280";
    private static final String TEXT_FAINT    = "#9CA3AF";
    private static final String BG            = "#F9FAFB";
    private static final String CARD          = "#FFFFFF";
    private static final String ACCENT_BG     = "#EEF2FF";
    private static final String BORDER        = "#E5E7EB";
    private static final String FOOTER_BG     = "#F3F4F6";
    private static final String SUCCESS       = "#15803D";
    private static final String DANGER        = "#B91C1C";

    private BaseEmailLayout() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Wraps {@code contentHtml} in the full branded layout.
     *
     * @param preheader   short preview text shown in inbox before opening (hidden in body)
     * @param contentHtml inner HTML content for the email body
     */
    public static String wrap(String preheader, String contentHtml) {
        int year = Year.now().getValue();
        String preheaderHtml = buildPreheader(preheader);

        return "<!DOCTYPE html>\n"
            + "<html lang=\"it\">\n"
            + "<head>\n"
            + "  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">\n"
            + "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n"
            + "  <title>Prenota24</title>\n"
            + "</head>\n"
            + "<body style=\"margin:0;padding:0;background-color:" + BG + ";font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;-webkit-text-size-adjust:100%;-ms-text-size-adjust:100%;\">\n"
            + preheaderHtml
            + "  <!-- outer -->\n"
            + "  <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\n"
            + "         style=\"background-color:" + BG + ";\">\n"
            + "    <tr>\n"
            + "      <td align=\"center\" style=\"padding:32px 16px;\">\n"
            + "        <!-- card -->\n"
            + "        <table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\n"
            + "               style=\"max-width:600px;width:100%;background-color:" + CARD + ";border-radius:12px;\n"
            + "                      overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.06),0 4px 8px rgba(0,0,0,0.04);\">\n"
            + "\n"
            + "          <!-- ── HEADER ── -->\n"
            + "          <tr>\n"
            + "            <td style=\"background:linear-gradient(135deg," + PRIMARY + " 0%," + PRIMARY_DARK + " 100%);\n"
            + "                        padding:28px 40px;text-align:center;\">\n"
            + "              <div style=\"display:inline-block;\">\n"
            + "                <span style=\"font-size:24px;font-weight:700;color:#FFFFFF;letter-spacing:-0.5px;\">Prenota</span>"
            + "<span style=\"font-size:24px;font-weight:700;color:rgba(255,255,255,0.65);letter-spacing:-0.5px;\">24</span>\n"
            + "              </div>\n"
            + "              <p style=\"margin:6px 0 0 0;font-size:11px;color:rgba(255,255,255,0.65);\n"
            + "                         letter-spacing:0.1em;text-transform:uppercase;\">Il tuo gestionale appuntamenti</p>\n"
            + "            </td>\n"
            + "          </tr>\n"
            + "\n"
            + "          <!-- ── BODY ── -->\n"
            + "          <tr>\n"
            + "            <td style=\"padding:36px 40px 32px 40px;color:" + TEXT + ";\">\n"
            + contentHtml + "\n"
            + "            </td>\n"
            + "          </tr>\n"
            + "\n"
            + "          <!-- ── DIVIDER ── -->\n"
            + "          <tr>\n"
            + "            <td style=\"padding:0 40px;\"><hr style=\"border:none;border-top:1px solid " + BORDER + ";margin:0;\"></td>\n"
            + "          </tr>\n"
            + "\n"
            + "          <!-- ── FOOTER ── -->\n"
            + "          <tr>\n"
            + "            <td style=\"background-color:" + FOOTER_BG + ";padding:24px 40px;\">\n"
            + "              <p style=\"margin:0 0 6px 0;font-size:12px;color:" + TEXT_MUTED + ";text-align:center;\">\n"
            + "                Hai ricevuto questa email da <strong>Prenota24</strong> in seguito a un&rsquo;attivit&agrave; sul tuo account.\n"
            + "              </p>\n"
            + "              <p style=\"margin:0 0 10px 0;font-size:12px;text-align:center;\">\n"
            + "                <a href=\"https://prenota24.com/privacy\" style=\"color:" + PRIMARY + ";text-decoration:none;\">Privacy Policy</a>\n"
            + "                &nbsp;&bull;&nbsp;\n"
            + "                <a href=\"mailto:supporto@prenota24.com\" style=\"color:" + PRIMARY + ";text-decoration:none;\">Contattaci</a>\n"
            + "              </p>\n"
            + "              <p style=\"margin:0;font-size:11px;color:" + TEXT_FAINT + ";text-align:center;\">\n"
            + "                &copy; " + year + " Prenota24. Tutti i diritti riservati.\n"
            + "              </p>\n"
            + "            </td>\n"
            + "          </tr>\n"
            + "\n"
            + "        </table>\n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>\n"
            + "</body>\n"
            + "</html>";
    }

    // ── Building blocks ───────────────────────────────────────────────────────

    /**
     * Renders a styled two-column info table (replaces ASCII box dividers).
     *
     * @param rows array of {@code [label, value]} pairs; both values are already HTML-escaped
     */
    public static String infoTable(String[][] rows) {
        var sb = new StringBuilder();
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\n")
          .append("       style=\"background-color:").append(ACCENT_BG).append(";border-radius:8px;margin:20px 0;\">\n");
        for (String[] row : rows) {
            sb.append("  <tr>\n")
              .append("    <td style=\"padding:9px 20px;font-size:13px;font-weight:600;color:").append(TEXT)
              .append(";width:38%;vertical-align:top;\">").append(row[0]).append("</td>\n")
              .append("    <td style=\"padding:9px 20px 9px 0;font-size:13px;color:").append(TEXT)
              .append(";vertical-align:top;\">").append(row[1]).append("</td>\n")
              .append("  </tr>\n");
        }
        sb.append("</table>\n");
        return sb.toString();
    }

    /**
     * Renders a large styled code block for OTP / verification codes.
     *
     * @param code the code string (digits)
     */
    public static String codeBlock(String code) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\n"
             + "       style=\"margin:24px 0;\">\n"
             + "  <tr>\n"
             + "    <td align=\"center\"\n"
             + "        style=\"background-color:" + ACCENT_BG + ";border-radius:10px;padding:28px 20px;\">\n"
             + "      <p style=\"margin:0 0 8px 0;font-size:12px;color:" + TEXT_MUTED + ";\n"
             + "                 letter-spacing:0.08em;text-transform:uppercase;\">Il tuo codice</p>\n"
             + "      <p style=\"margin:0;font-size:40px;font-weight:700;color:" + PRIMARY + ";\n"
             + "                 letter-spacing:0.22em;font-family:monospace,monospace;\">" + code + "</p>\n"
             + "    </td>\n"
             + "  </tr>\n"
             + "</table>\n";
    }

    /**
     * Renders a primary CTA button. Compatible with all major email clients.
     *
     * @param label button text
     * @param url   destination URL
     */
    public static String ctaButton(String label, String url) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\n"
             + "       style=\"margin:28px 0 0 0;\">\n"
             + "  <tr>\n"
             + "    <td align=\"center\" style=\"border-radius:8px;background-color:" + PRIMARY + ";\">\n"
             + "      <a href=\"" + url + "\" target=\"_blank\"\n"
             + "         style=\"display:inline-block;padding:14px 36px;font-size:15px;font-weight:600;\n"
             + "                color:#FFFFFF;text-decoration:none;border-radius:8px;\n"
             + "                background-color:" + PRIMARY + ";letter-spacing:0.01em;\">"
             + label
             + "      </a>\n"
             + "    </td>\n"
             + "  </tr>\n"
             + "</table>\n";
    }

    /**
     * Returns a success-coloured inline span.
     */
    public static String successBadge(String text) {
        return "<span style=\"color:" + SUCCESS + ";font-weight:600;\">" + text + "</span>";
    }

    /**
     * Returns a danger-coloured inline span.
     */
    public static String dangerBadge(String text) {
        return "<span style=\"color:" + DANGER + ";font-weight:600;\">" + text + "</span>";
    }

    /**
     * Escapes HTML special characters in user-supplied strings to prevent injection.
     */
    public static String e(String text) {
        if (text == null) return "";
        return text
                .replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#39;");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String buildPreheader(String preheader) {
        if (preheader == null || preheader.isBlank()) return "";
        return "  <div style=\"display:none;max-height:0;overflow:hidden;font-size:1px;color:" + BG + ";\">"
             + e(preheader)
             + "</div>\n";
    }
}
