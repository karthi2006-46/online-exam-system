package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin payload for assigning courses to a faculty member.
 */
@Data
@NoArgsConstructor
public class AssignCoursesRequest {

    private List<Long> courseIds = new ArrayList<>();
}
