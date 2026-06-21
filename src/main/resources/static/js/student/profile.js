async function loadProfile() {

    const userId = localStorage.getItem("userId");

    try {

        const response = await fetch(
            `http://localhost:8080/api/auth/profile/${userId}`
        );

        const result = await response.json();

        const user = result.data;

        document.getElementById("firstName").innerText =
            user.firstName;

        document.getElementById("lastName").innerText =
            user.lastName;

        document.getElementById("email").innerText =
            user.email;

        document.getElementById("phone").innerText =
            user.phone;

        document.getElementById("address").innerText =
            user.address;

        document.getElementById("role").innerText =
            user.roleName;

        document.getElementById("status").innerText =
            user.active ? "Active" : "Inactive";

    } catch(error){
        console.error(error);
    }
}

loadProfile();