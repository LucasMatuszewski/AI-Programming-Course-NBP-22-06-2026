package pl.nbp.copilot.backend.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import pl.nbp.copilot.backend.cases.EquipmentCategory;
import pl.nbp.copilot.backend.cases.RequestType;

import java.time.LocalDate;
import java.util.Map;

/**
 * Multipart form DTO for {@code POST /api/cases}.
 *
 * <p>The frontend sends English wire values for {@code category} (e.g. {@code SMARTPHONES}).
 * These are mapped to the Polish-named {@link EquipmentCategory} enum constants via
 * {@link #toEquipmentCategory()}.
 *
 * <p>Field validation:
 * <ul>
 *   <li>{@code requestType} — not blank</li>
 *   <li>{@code category} — not blank; must be a known wire value</li>
 *   <li>{@code modelName} — not blank; max 100 characters</li>
 *   <li>{@code purchaseDate} — valid ISO date; not in the future</li>
 *   <li>{@code image} — not null</li>
 * </ul>
 *
 * <p>Cross-field validation (server-side): {@code reason} is required when
 * {@code requestType} is {@code COMPLAINT}.
 */
public class CaseFormDto {

    /**
     * Maps English wire category values (sent by the frontend) to {@link EquipmentCategory} domain constants.
     */
    private static final Map<String, EquipmentCategory> CATEGORY_WIRE_MAP = Map.ofEntries(
            Map.entry("SMARTPHONES",      EquipmentCategory.SMARTFONY),
            Map.entry("LAPTOPS",          EquipmentCategory.LAPTOPY_KOMPUTERY),
            Map.entry("TABLETS",          EquipmentCategory.TABLETY),
            Map.entry("TVS_MONITORS",     EquipmentCategory.TELEWIZORY_MONITORY),
            Map.entry("AUDIO",            EquipmentCategory.AUDIO),
            Map.entry("SMALL_APPLIANCES", EquipmentCategory.AGD_MALE),
            Map.entry("LARGE_APPLIANCES", EquipmentCategory.AGD_DUZE),
            Map.entry("GAMING",           EquipmentCategory.KONSOLE_GAMING),
            Map.entry("ACCESSORIES",      EquipmentCategory.AKCESORIA_PERYFERIA),
            Map.entry("OTHER",            EquipmentCategory.INNE)
    );

    @NotBlank(message = "Typ zgłoszenia jest wymagany")
    private String requestType;

    @NotBlank(message = "Kategoria sprzętu jest wymagana")
    private String category;

    @NotBlank(message = "Nazwa modelu jest wymagana")
    @Size(max = 100, message = "Nazwa modelu nie może przekraczać 100 znaków")
    private String modelName;

    @NotBlank(message = "Data zakupu jest wymagana")
    private String purchaseDate;

    private String reason;

    @NotNull(message = "Zdjęcie jest wymagane")
    private MultipartFile image;

    // --- Constructors ---

    /** Default constructor for Spring MVC binding. */
    public CaseFormDto() {
    }

    // --- Accessors ---

    /** Returns the request type wire value. */
    public String getRequestType() {
        return requestType;
    }

    /** Sets the request type wire value. */
    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    /** Returns the category wire value. */
    public String getCategory() {
        return category;
    }

    /** Sets the category wire value. */
    public void setCategory(String category) {
        this.category = category;
    }

    /** Returns the model name. */
    public String getModelName() {
        return modelName;
    }

    /** Sets the model name. */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /** Returns the purchase date as a raw string. */
    public String getPurchaseDate() {
        return purchaseDate;
    }

    /** Sets the purchase date as a raw string. */
    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    /** Returns the defect/return reason. */
    public String getReason() {
        return reason;
    }

    /** Sets the defect/return reason. */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /** Returns the uploaded image. */
    public MultipartFile getImage() {
        return image;
    }

    /** Sets the uploaded image. */
    public void setImage(MultipartFile image) {
        this.image = image;
    }

    // --- Domain conversion helpers ---

    /**
     * Parses the {@code requestType} wire value to {@link RequestType}.
     *
     * @return parsed enum
     * @throws IllegalArgumentException if the wire value is not a valid {@code RequestType}
     */
    public RequestType toRequestType() {
        try {
            return RequestType.valueOf(requestType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Nieznany typ zgłoszenia: '" + requestType + "'");
        }
    }

    /**
     * Maps the English {@code category} wire value to the domain {@link EquipmentCategory}.
     *
     * @return matched {@link EquipmentCategory}
     * @throws CategoryMappingException if the wire value is unknown
     */
    public EquipmentCategory toEquipmentCategory() {
        String key = (category == null) ? "" : category.trim().toUpperCase();
        EquipmentCategory mapped = CATEGORY_WIRE_MAP.get(key);
        if (mapped == null) {
            throw new CategoryMappingException(
                    "Nieznana kategoria sprzętu: '" + category + "'");
        }
        return mapped;
    }

    /**
     * Parses the {@code purchaseDate} ISO string to {@link java.time.LocalDate}.
     *
     * @return parsed date
     * @throws IllegalArgumentException if the string is not a valid ISO date
     */
    public LocalDate toPurchaseDate() {
        try {
            return LocalDate.parse(purchaseDate.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Nieprawidłowy format daty zakupu: '" + purchaseDate + "'. Wymagany format: YYYY-MM-DD");
        }
    }
}
