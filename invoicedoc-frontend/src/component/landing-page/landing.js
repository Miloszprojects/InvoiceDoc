export function initLanding() {
    const root = document.querySelector(".landing");
    if (!root) return;

    document.title = "InvoiceApp – Online invoicing for teams";

    const roleCards = root.querySelectorAll(".role-tag");
    roleCards.forEach((card) => {
        card.addEventListener("mouseenter", () => {
            card.classList.add("role-tag--active");
        });
        card.addEventListener("mouseleave", () => {
            card.classList.remove("role-tag--active");
        });
    });
}
