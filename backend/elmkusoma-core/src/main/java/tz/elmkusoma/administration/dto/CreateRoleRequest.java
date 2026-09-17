package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreateRoleRequest {

    @NotBlank(message = "Role name is required")
    private String name;

    @NotBlank(message = "Display name is required")
    private String displayName;

    private String description;
    private List<String> permissions;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }

    public static CreateRoleRequest of(String name, String displayName, String description, List<String> permissions) {
        CreateRoleRequest req = new CreateRoleRequest();
        req.name = name;
        req.displayName = displayName;
        req.description = description;
        req.permissions = permissions;
        return req;
    }
}