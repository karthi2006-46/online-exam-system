package com.lms.dto.admin;

import com.lms.dto.UserDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned after creating a faculty account.
 * The temporary password is only present when the server generated one.
 */
@Data
@NoArgsConstructor
public class AdminFacultyAccountDTO {

    private UserDTO faculty;
    private boolean passwordGenerated;
    private String temporaryPassword;
}
