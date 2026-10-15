async function loadMaterials() {

    try {

        const studentId =
            localStorage.getItem("userId");

        const courseId =
            localStorage.getItem("courseId");

        const token =
            localStorage.getItem("token");

        console.log("Student ID:", studentId);
        console.log("Course ID:", courseId);

        if (!studentId) {

            alert("Please login again");
            return;
        }

        if (!courseId) {

            alert("Course not selected");
            return;
        }


        // ==============================
        // LOAD COURSE MATERIALS
        // ==============================

        const materialsResponse =
            await fetch(
                `http://localhost:8080/api/materials/course/${courseId}`,
                {
                    headers: {
                        "Authorization":
                            `Bearer ${token}`
                    }
                }
            );

        if (!materialsResponse.ok) {

            throw new Error(
                "Failed to load materials"
            );
        }

        const materialsResult =
            await materialsResponse.json();

        console.log(
            "Materials API:",
            materialsResult
        );

        const materials =
            materialsResult.data || [];


        // ==============================
        // LOAD COMPLETED MATERIALS
        // ==============================

        const completedResponse =
            await fetch(
                `http://localhost:8080/api/material-progress/student/${studentId}`,
                {
                    headers: {
                        "Authorization":
                            `Bearer ${token}`
                    }
                }
            );

        if (!completedResponse.ok) {

            throw new Error(
                "Failed to load completed materials"
            );
        }

        const completedIds =
            await completedResponse.json();

        console.log(
            "Completed Material IDs:",
            completedIds
        );


        // ==============================
        // CONTAINER
        // ==============================

        const container =
            document.getElementById(
                "materialsContainer"
            );

        if (!container) {

            console.error(
                "materialsContainer not found"
            );

            return;
        }

        container.innerHTML = "";


        // ==============================
        // NO MATERIALS
        // ==============================

        if (materials.length === 0) {

            container.innerHTML = `
                <p style="
                    font-size:18px;
                    color:#666;
                ">
                    No study materials available
                    for this course.
                </p>
            `;

            return;
        }


        // ==============================
        // DISPLAY MATERIALS
        // ==============================

        materials.forEach(material => {

            const completed =
                completedIds.includes(
                    material.id
                );

            container.innerHTML += `

                <div class="material-card">

                    <div class="material-title">
                        ${material.title}
                    </div>

                    <div class="material-type">
                        ${material.fileType}
                    </div>

                    <div class="material-desc">
                        ${material.description || ""}
                    </div>

                    <button
                        class="download-btn"
                        onclick="
                            window.open(
                                '${material.fileUrl}',
                                '_blank'
                            )
                        ">
                        Open Material
                    </button>

                    ${
                        completed

                        ?

                        `
                        <button
                            disabled
                            style="
                                margin-left:5px;
                                opacity:0.7;
                            ">
                            Completed ✅
                        </button>
                        `

                        :

                        `
                        <button
                            onclick="
                                completeMaterial(
                                    ${material.id}
                                )
                            ">
                            Mark Complete
                        </button>
                        `
                    }

                </div>

            `;

        });

    } catch(error) {

        console.error(
            "Load Materials Error:",
            error
        );

        alert(
            "Unable to load study materials"
        );
    }
}



// ======================================
// COMPLETE MATERIAL
// ======================================

async function completeMaterial(materialId) {

    try {

        const studentId =
            localStorage.getItem("userId");

        const token =
            localStorage.getItem("token");

        if (!studentId) {

            alert(
                "Please login again"
            );

            return;
        }


        console.log(
            "Completing Material:",
            materialId
        );

        console.log(
            "Student:",
            studentId
        );


        const response =
            await fetch(
                `http://localhost:8080/api/material-progress/complete/${materialId}?studentId=${studentId}`,
                {
                    method: "POST",

                    headers: {
                        "Authorization":
                            `Bearer ${token}`
                    }
                }
            );


        if (response.ok) {

            alert(
                "Material Completed Successfully"
            );

            // Reload materials
            // Completed button will appear
            await loadMaterials();

        } else {

            const errorText =
                await response.text();

            console.error(
                "Complete Material Error:",
                errorText
            );

            alert(
                "Failed to Complete Material"
            );
        }

    } catch(error) {

        console.error(
            "Complete Material Exception:",
            error
        );

        alert(
            "Failed to Complete Material"
        );
    }
}


// ======================================
// PAGE LOAD
// ======================================

loadMaterials();