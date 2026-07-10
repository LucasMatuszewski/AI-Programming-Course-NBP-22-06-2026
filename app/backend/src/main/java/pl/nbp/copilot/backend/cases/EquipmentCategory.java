package pl.nbp.copilot.backend.cases;

/**
 * Predefined equipment categories per PRD §8 (exactly 10 values).
 *
 * <p>Each constant carries a Polish {@link #getDisplayLabel() displayLabel} used in UI and
 * agent prompts.
 */
public enum EquipmentCategory {

    /** Smartfony. */
    SMARTFONY("Smartfony"),

    /** Laptopy / Komputery. */
    LAPTOPY_KOMPUTERY("Laptopy / Komputery"),

    /** Tablety. */
    TABLETY("Tablety"),

    /** Telewizory / Monitory. */
    TELEWIZORY_MONITORY("Telewizory / Monitory"),

    /** Audio (słuchawki, głośniki). */
    AUDIO("Audio (słuchawki, głośniki)"),

    /** AGD małe. */
    AGD_MALE("AGD małe"),

    /** AGD duże. */
    AGD_DUZE("AGD duże"),

    /** Konsole / Gaming. */
    KONSOLE_GAMING("Konsole / Gaming"),

    /** Akcesoria / Peryferia. */
    AKCESORIA_PERYFERIA("Akcesoria / Peryferia"),

    /** Inne. */
    INNE("Inne");

    private final String displayLabel;

    EquipmentCategory(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    /**
     * Returns the Polish display label for this category.
     *
     * @return non-null, non-blank Polish category name
     */
    public String getDisplayLabel() {
        return displayLabel;
    }
}
