document.addEventListener("DOMContentLoaded", () => {
    const toggle = document.querySelector(".nav-toggle");
    const nav = document.getElementById("site-nav");
    if (toggle && nav) {
        toggle.addEventListener("click", () => {
            const open = toggle.getAttribute("aria-expanded") !== "true";
            toggle.setAttribute("aria-expanded", String(open));
            nav.classList.toggle("open", open);
        });
        document.addEventListener("keydown", event => {
            if (event.key === "Escape") {
                nav.classList.remove("open");
                toggle.setAttribute("aria-expanded", "false");
            }
        });
    }

    const signup = document.querySelector("[data-signup]");
    if (signup) {
        const password = signup.querySelector("#passwd");
        const confirmation = signup.querySelector("#confirmPasswd");
        const validate = () => confirmation.setCustomValidity(
            password.value === confirmation.value ? "" : "Les mots de passe ne correspondent pas."
        );
        password.addEventListener("input", validate);
        confirmation.addEventListener("input", validate);
    }

    const booking = document.querySelector("[data-booking]");
    if (booking) {
        const days = booking.querySelector("#duree");
        const start = booking.querySelector("#dteDeb");
        const price = Number(booking.dataset.price);
        const update = () => {
            const duration = Number(days.value);
            const valid = Number.isInteger(duration) && duration >= 1 && duration <= 30;
            booking.querySelector("[data-days]").textContent = valid ? duration : "—";
            booking.querySelector("[data-total]").textContent = valid
                ? new Intl.NumberFormat("fr-FR", {
                    style: "currency", currency: "EUR", minimumFractionDigits: 2, maximumFractionDigits: 2
                }).format(price * duration)
                : "—";
            const date = new Date(start.value + "T12:00:00");
            if (valid && !Number.isNaN(date.getTime())) {
                date.setDate(date.getDate() + duration);
                booking.querySelector("[data-return]").textContent = new Intl.DateTimeFormat("fr-FR", {
                    day: "numeric", month: "long", year: "numeric"
                }).format(date);
            } else {
                booking.querySelector("[data-return]").textContent = "—";
            }
        };
        days.addEventListener("input", update);
        start.addEventListener("input", update);
        update();
    }
});
