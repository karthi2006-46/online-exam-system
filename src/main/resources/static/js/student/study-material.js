async function loadMaterials() {

    try {

        const response =
            await fetch(
                "http://localhost:8080/api/materials/course/1"
            );

        const result =
            await response.json();

        const materials =
            result.data;

        const container =
            document.getElementById(
                "materialsContainer"
            );

        container.innerHTML = "";

        materials.forEach(material => {

            container.innerHTML += `

            <div class="material-card">

                <div class="material-title">
                    ${material.title}
                </div>

                <div class="material-type">
                    ${material.fileType}
                </div>

                <div class="material-desc">
                    ${material.description}
                </div>

                <button
                    class="download-btn"
                    onclick="window.open('${material.fileUrl}')">
                    Open Material
                </button>

            </div>

            `;
        });

    } catch(error){

        console.error(error);

    }
}

loadMaterials();